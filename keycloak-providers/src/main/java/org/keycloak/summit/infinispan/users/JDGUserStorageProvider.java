/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.summit.infinispan.users;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.federated.UserBrokerLinkFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JDGUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        UserRegistrationProvider,
        UserQueryProvider,
        CredentialInputUpdater,
        CredentialInputValidator,
        OnUserCache,
        UserBrokerLinkFederatedStorage {

    private final KeycloakSession session;
    private final ComponentModel component;
    protected final JDGUserStorageTransaction transaction;


    private static final Logger logger = Logger.getLogger(JDGUserStorageProvider.class);
    public static final String ID_CACHE_KEY = "id.";
    //public static final String USERNAME_CACHE_KEY = "username.";
    public static final String EMAIL_CACHE_KEY = "email.";
    public static final String FED_CACHE_KEY = "fed.";

    public static final String PASSWORD_CACHE_KEY = "JDGUser.password";

    public JDGUserStorageProvider() {
        this.session = null;
        this.component = null;
        this.transaction = null;
    }

    public JDGUserStorageProvider(KeycloakSession session, ComponentModel component, RemoteCache remoteCache) {
        this.session = session;
        this.component = component;
        this.transaction = new JDGUserStorageTransaction(remoteCache);

        session.getTransactionManager().enlistAfterCompletion(this.transaction);
    }


    @Override
    public void preRemove(RealmModel realm) {

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    @Override
    public void close() {
    }

    // id is something like "f:123321-f4546-e884-ffef:john@email.cz"
    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        logger.debugf("getUserById: %s", id);
        String persistenceId = StorageId.externalId(id);
        return getUserByPersistenceId(persistenceId, realm);
    }

    // persistenceId is something like "john@email.cz"
    protected UserModel getUserByPersistenceId(String persistenceId, RealmModel realm) {
        JDGUserEntity entity = getUserEntity(persistenceId);
        if (entity == null) {
            logger.infof("could not find user by id: %s", persistenceId);
            return null;
        }
        return new JDGUserAdapter(session, realm, component, this, entity);
    }

    protected String getByIdCacheKey(String id) {
        return ID_CACHE_KEY + id;
    }

    // persistenceId is something like "john@email.cz"
    private JDGUserEntity getUserEntity(String persistenceId) {
        String cacheId = getByIdCacheKey(persistenceId);
        return (JDGUserEntity) transaction.get(cacheId);
    }

//    private String getByUsernameCacheKey(String username) {
//        return USERNAME_CACHE_KEY + username;
//    }

    protected String getByEmailCacheKey(String email) {
        return EMAIL_CACHE_KEY + email;
    }

    protected String getByFedLinkCacheKey(String identityProvider, String fedUserId) {
        return FED_CACHE_KEY + identityProvider + "." + fedUserId;
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        logger.debugf("getUserByUsername: %s", username);
        // ID is username
        return getUserByPersistenceId(username, realm);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        // In most cases for this impl, ID will be email. Just in case that email changed in account mgmt, it may be different
        logger.debugf("getUserByEmail: %s", email);
        UserModel user = getUserByPersistenceId(email, realm);
        if (user != null) {
            return user;
        }

        logger.infof("not found user by email '%s' . Trying fallback", email);
        String cacheKey = getByEmailCacheKey(email);
        String id = (String) transaction.get(cacheKey);
        if (id == null) {
            return null;
        }
        return getUserByPersistenceId(id, realm);
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        if (getUserByPersistenceId(username, realm) != null) {
            throw new ModelDuplicateException("User '" + username + "' already exists");
        }

        String cacheKey = getByIdCacheKey(username);
        JDGUserEntity entity = new JDGUserEntity();
        entity.setId(username);
        entity.setUsername(username);

        transaction.create(cacheKey, entity);
        logger.debugf("added user: %s", username);
        return new JDGUserAdapter(session, realm, component, this, entity);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        String persistenceId = StorageId.externalId(user.getId());

        String idCacheKey = getByIdCacheKey(persistenceId);
        transaction.remove(idCacheKey);

        if (user.getEmail() != null) {
            String emailCacheKey = getByEmailCacheKey(user.getEmail());
            transaction.remove(emailCacheKey);
        }

        return true;
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        String password = ((JDGUserAdapter)delegate).getPassword();
        if (password != null) {
            user.getCachedWith().put(PASSWORD_CACHE_KEY, password);
        }
        // TODO:mposolda totp
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return CredentialModel.PASSWORD.equals(credentialType);
        // TODO:mposolda totp
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;

        UserCredentialModel cred = (UserCredentialModel)input;
        JDGUserAdapter adapter = getUserAdapter(user);
        adapter.setPassword(cred.getValue());

        // TODO:mposolda totp

        return true;
    }

    public JDGUserAdapter getUserAdapter(UserModel user) {
        JDGUserAdapter adapter = null;
        if (user instanceof CachedUserModel) {
            adapter = (JDGUserAdapter)((CachedUserModel)user).getDelegateForUpdate();
        } else {
            adapter = (JDGUserAdapter)user;
        }
        return adapter;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return;

        getUserAdapter(user).setPassword(null);

        // TODO:mposolda totp
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        if (getUserAdapter(user).getPassword() != null) {
            Set<String> set = new HashSet<>();
            set.add(CredentialModel.PASSWORD);
            return set;
        } else {
            return Collections.emptySet();
        }

        // TODO:mposolda totp
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType) && getPassword(user) != null;

        // TODO:mposolda totp
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;
        UserCredentialModel cred = (UserCredentialModel)input;
        String password = getPassword(user);
        return password != null && password.equals(cred.getValue());

        // TODO:mposolda totp
    }

    public String getPassword(UserModel user) {
        String password = null;
        if (user instanceof CachedUserModel) {
            password = (String)((CachedUserModel)user).getCachedWith().get(PASSWORD_CACHE_KEY);
        } else if (user instanceof JDGUserAdapter) {
            password = ((JDGUserAdapter)user).getPassword();
        }
        return password;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return loadAllUsers(realm).size();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return loadAllUsers(realm);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        List<UserModel> users = loadAllUsers(realm);
        return paginateUsers(users.stream(), firstResult, maxResults);
    }

    private List<UserModel> paginateUsers(Stream<UserModel> stream, int firstResult, int maxResults) {
        if (firstResult > -1) {
            stream = stream.skip(firstResult);
        }
        if (maxResults > -1) {
            stream = stream.limit(maxResults);
        }

        return stream.collect(Collectors.toList());
    }

    private List<UserModel> users = null;

    private List<UserModel> loadAllUsers(RealmModel realm) {
        if (users == null) {
            // TODO:mposolda Performance killer...
            logger.infof("Calling loadAllUsers!");
            this.users = (List<UserModel>) transaction.remoteCache.getBulk().values().stream().filter((Object value) -> {

                return value instanceof JDGUserEntity;

            }).map((Object obj) -> {

                JDGUserEntity entity = (JDGUserEntity) obj;
                return new JDGUserAdapter(session, realm, component, JDGUserStorageProvider.this, entity);

            }).collect(Collectors.toList());
        }

        return users;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        List<UserModel> users = loadAllUsers(realm);

        Stream<UserModel> stream = users.stream().filter((UserModel user) -> {

            boolean contains = user.getUsername().contains(search);
            contains = contains || (user.getEmail() != null && user.getEmail().contains(search));
            contains = contains || (getFullName(user).contains(search));

            return contains;

        });

        return paginateUsers(stream, firstResult, maxResults);
    }

    private String getFullName(UserModel user) {
        if (user.getFirstName() == null && user.getLastName() == null) return "";

        if (user.getFirstName() == null) return user.getLastName();
        if (user.getLastName() == null) return user.getFirstName();
        return user.getFirstName() + " " + user.getLastName();
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getRoleMembers(RealmModel realm, RoleModel role, int firstResult, int maxResults) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getRoleMembers(RealmModel realm, RoleModel role) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return Collections.EMPTY_LIST;
    }


    // FEDERATION LINKS


    @Override
    public String getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        String fedLinkCacheKey = getByFedLinkCacheKey(socialLink.getIdentityProvider(), socialLink.getUserId());
        return (String) transaction.get(fedLinkCacheKey);
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel socialLink) {
        String persistenceId = StorageId.externalId(userId);
        JDGUserEntity entity = getUserEntity(persistenceId);
        if (entity == null) {
            logger.info("addFederatedIdentity: could not find user by id: " + persistenceId);
            return;
        }

        // Update user
        JDGFederatedLinkEntity jdgLink = fromModelLink(socialLink);
        entity.getFederationLinks().add(jdgLink);
        transaction.replace(getByIdCacheKey(persistenceId), entity);

        // Add the cacheKey with the link
        String fedLinkCacheKey = getByFedLinkCacheKey(socialLink.getIdentityProvider(), socialLink.getUserId());
        transaction.create(fedLinkCacheKey, userId);
    }

    private FederatedIdentityModel fromJDGLink(JDGFederatedLinkEntity jdgLink) {
        return new FederatedIdentityModel(jdgLink.getIdentityProvider(), jdgLink.getUserId(), jdgLink.getUserName());
    }

    private JDGFederatedLinkEntity fromModelLink(FederatedIdentityModel modelLink) {
        return new JDGFederatedLinkEntity(modelLink.getUserId(), modelLink.getIdentityProvider(), modelLink.getUserName());
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, String userId, String socialProvider) {
        String persistenceId = StorageId.externalId(userId);
        JDGUserEntity entity = getUserEntity(persistenceId);
        if (entity == null) {
            logger.info("removeFederatedIdentity: could not find user by id: " + persistenceId);
            return false;
        }

        for (JDGFederatedLinkEntity jdgLink : entity.getFederationLinks()) {
            if (jdgLink.getIdentityProvider().equals(socialProvider)) {
                // Update user
                entity.getFederationLinks().remove(jdgLink);
                transaction.replace(getByIdCacheKey(persistenceId), entity);

                // Remove fedLink item from cache
                String fedLinkCacheKey = getByFedLinkCacheKey(jdgLink.getIdentityProvider(), jdgLink.getUserId());
                transaction.remove(fedLinkCacheKey);

                return true;
            }
        }

        return false;
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel federatedIdentityModel) {
        // Not needed for now as we don't store social tokens
    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(String userId, RealmModel realm) {
        String persistenceId = StorageId.externalId(userId);
        JDGUserEntity entity = getUserEntity(persistenceId);
        if (entity == null) {
            logger.info("getFederatedIdentities: could not find user by id: " + persistenceId);
            return Collections.EMPTY_SET;
        }

        Set<FederatedIdentityModel> modelLinks = new HashSet<>();
        for (JDGFederatedLinkEntity jdgLink : entity.getFederationLinks()) {
            modelLinks.add(fromJDGLink(jdgLink));
        }
        return modelLinks;
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(String userId, String socialProvider, RealmModel realm) {
        String persistenceId = StorageId.externalId(userId);
        JDGUserEntity entity = getUserEntity(persistenceId);
        if (entity == null) {
            logger.info("getFederatedIdentity: could not find user by id: " + persistenceId);
            return null;
        }

        for (JDGFederatedLinkEntity jdgLink : entity.getFederationLinks()) {
            if (jdgLink.getIdentityProvider().equals(socialProvider)) {
                return fromJDGLink(jdgLink);
            }
        }

        return null;
    }
}
