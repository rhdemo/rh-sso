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

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageManager;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.jpa.JpaUserFederatedStorageProvider;

/**
 * Uses JPA for all the stuff with the exception of federated identities (Social links). Those are saved in JDG remoteCache
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JDGJpaUserFederatedStorageProvider extends JpaUserFederatedStorageProvider {

    private final KeycloakSession session;

    public JDGJpaUserFederatedStorageProvider(KeycloakSession session, EntityManager em) {
        super(session, em);
        this.session = session;
    }


    @Override
    public String getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        // Hardcoded for JDG provider if it exists inside realm. In this case JPA is not tried. This should be sufficient for demo purposes and have best performance
        JDGUserStorageProvider delegate = getJDGStorageProvider(realm);
        if (delegate == null) {
            return super.getUserByFederatedIdentity(socialLink, realm);
        } else {
            return delegate.getUserByFederatedIdentity(socialLink, realm);
        }
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel socialLink) {
        JDGUserStorageProvider delegate = getJDGProviderByUserId(realm, userId);
        if (delegate == null) {
            super.addFederatedIdentity(realm, userId, socialLink);
        } else {
            delegate.addFederatedIdentity(realm, userId, socialLink);
        }
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, String userId, String socialProvider) {
        JDGUserStorageProvider delegate = getJDGProviderByUserId(realm, userId);
        if (delegate == null) {
            return super.removeFederatedIdentity(realm, userId, socialProvider);
        } else {
            return delegate.removeFederatedIdentity(realm, userId, socialProvider);
        }
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel federatedIdentityModel) {
        JDGUserStorageProvider delegate = getJDGProviderByUserId(realm, userId);
        if (delegate == null) {
            super.updateFederatedIdentity(realm, userId, federatedIdentityModel);
        } else {
            delegate.updateFederatedIdentity(realm, userId, federatedIdentityModel);
        }
    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(String userId, RealmModel realm) {
        JDGUserStorageProvider delegate = getJDGProviderByUserId(realm, userId);
        if (delegate == null) {
            return super.getFederatedIdentities(userId, realm);
        } else {
            return delegate.getFederatedIdentities(userId, realm);
        }
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(String userId, String socialProvider, RealmModel realm) {
        JDGUserStorageProvider delegate = getJDGProviderByUserId(realm, userId);
        if (delegate == null) {
            return super.getFederatedIdentity(userId, socialProvider, realm);
        } else {
            return delegate.getFederatedIdentity(userId, socialProvider, realm);
        }
    }

    private JDGUserStorageProvider getJDGStorageProvider(RealmModel realm) {
        List<JDGUserStorageProvider> jdgProviders = UserStorageManager.getStorageProviders(session, realm, JDGUserStorageProvider.class);
        if (jdgProviders.size() == 0) {
            return null;
        } else {
            return jdgProviders.get(0);
        }
    }

    private JDGUserStorageProvider getJDGProviderByUserId(RealmModel realm, String userId) {
        String providerId = new StorageId(userId).getProviderId();
        if (providerId == null) {
            return null;
        }

        UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
        if (provider instanceof JDGUserStorageProvider) {
            return (JDGUserStorageProvider) provider;
        } else {
            return null;
        }
    }
}
