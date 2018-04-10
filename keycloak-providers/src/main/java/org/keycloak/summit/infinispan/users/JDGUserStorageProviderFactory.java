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

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.cache.infinispan.CacheManager;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;
import org.keycloak.storage.UserStorageProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JDGUserStorageProviderFactory implements UserStorageProviderFactory<JDGUserStorageProvider> {

    private static final Logger logger = Logger.getLogger(JDGUserStorageProviderFactory.class);

    public static final String PROVIDER_ID = "jdg";
    public static final String CACHE_NAME = "userStorage";

    private volatile boolean initialized = false;
    private RemoteCache remoteCache;

    @Override
    public JDGUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        if (initialized == false) {
            synchronized (this) {
                if (initialized == false) {
                    remoteCache = getRemoteCache(session);
                    initialized = true;
                }
            }
        }

        if (remoteCache != null) {
            return new JDGUserStorageProvider(session, model, remoteCache);
        } else {
            return new EmptyUserStorageProvider();
        }
    }

    static RemoteCache getRemoteCache(KeycloakSession session) {
        InfinispanConnectionProvider ispn = session.getProvider(InfinispanConnectionProvider.class);

        defineUserStorageCacheConfig(ispn);

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(JDGUserStorageProviderFactory.class.getClassLoader());
        try {
            Cache cache = ispn.getCache(CACHE_NAME);
            if (cache == null) {
                logger.warnf("Cache '%s' not available", CACHE_NAME);
                return null;
            }
            RemoteCache remoteCache = InfinispanUtil.getRemoteCache(cache);
            if (remoteCache == null) {
                logger.warnf("Cache '%s' must be configured with remoteStore", CACHE_NAME);
                return null;
            }

            logger.infof("Remote cache '%s' available.", CACHE_NAME);
            return remoteCache;
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


    // Workaround: userStorage cache is not in the dependencies. We need to define it manually
    private static void defineUserStorageCacheConfig(InfinispanConnectionProvider ispn) {
        EmbeddedCacheManager mgr = ispn.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME).getCacheManager();
        Configuration cfgTemplate = mgr.getCacheConfiguration(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

        ConfigurationBuilder builder = new ConfigurationBuilder().read(cfgTemplate);

        RemoteStoreConfigurationBuilder remoteStoreBuilder = (RemoteStoreConfigurationBuilder) builder.persistence().stores().get(0);
        remoteStoreBuilder.remoteCacheName(CACHE_NAME)
                .marshaller("org.keycloak.summit.infinispan.users.UpdatedKeycloakHotrodMarshallerFactory");


        Configuration newCfg = builder.build();
        mgr.defineConfiguration(CACHE_NAME, newCfg);
    }

}
