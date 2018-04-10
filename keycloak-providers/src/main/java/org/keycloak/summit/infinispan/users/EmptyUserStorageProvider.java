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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.federated.UserBrokerLinkFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EmptyUserStorageProvider extends JDGUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        UserRegistrationProvider,
        UserQueryProvider,
        CredentialInputUpdater,
        CredentialInputValidator,
        OnUserCache,
        UserBrokerLinkFederatedStorage {

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return false;
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        return false;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {

    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return Collections.emptySet();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return false;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        return false;
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        return null;
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel socialLink) {

    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, String userId, String socialProvider) {
        return false;
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel federatedIdentityModel) {

    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(String userId, RealmModel realm) {
        return null;
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(String userId, String socialProvider, RealmModel realm) {
        return null;
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        return null;
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        return null;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return 0;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return Collections.emptyList();
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return false;
    }
}
