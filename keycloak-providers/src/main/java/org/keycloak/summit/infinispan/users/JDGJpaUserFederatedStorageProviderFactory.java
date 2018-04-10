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

import javax.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.storage.jpa.JpaUserFederatedStorageProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JDGJpaUserFederatedStorageProviderFactory extends JpaUserFederatedStorageProviderFactory {

    public static final String PROVIDER_ID = "jdg-jpa";

    @Override
    public UserFederatedStorageProvider create(KeycloakSession session) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        return new JDGJpaUserFederatedStorageProvider(session, em);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
