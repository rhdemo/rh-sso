package org.keycloak.summit.infinispan;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProvider;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.initializer.InfinispanCacheInitializer;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;
import org.keycloak.models.sessions.infinispan.util.InfinispanKeyGenerator;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

/**
 * Infinispan session factory, which is able to use Infinispan Server 9.2.0.Final as the JDG server
 *
 * Workaround for https://issues.jboss.org/browse/KEYCLOAK-5979
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UpdatedInfinispanUserSessionProviderFactory extends InfinispanUserSessionProviderFactory implements UserSessionProviderFactory {

    private static final Logger log = Logger.getLogger(UpdatedInfinispanUserSessionProviderFactory.class);

    public static final String UPDATED_PROVIDER_ID = "updated-infinispan";

    private Config.Scope config;

    @Override
    public String getId() {
        return UPDATED_PROVIDER_ID;
    }

    @Override
    public void init(Config.Scope config) {
        super.init(config);
        this.config = config;
    }

    @Override
    public void postInit(final KeycloakSessionFactory factory) {

        factory.register(new ProviderEventListener() {

            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof PostMigrationEvent) {
                    KeycloakSession session = ((PostMigrationEvent) event).getSession();

                    // keyGenerator = new InfinispanKeyGenerator();
                    // Need to use reflection :/
                    try {
                        InfinispanKeyGenerator keyGenerator = new InfinispanKeyGenerator();
                        Field keyGeneratorField = Reflections.findDeclaredField(InfinispanUserSessionProviderFactory.class, "keyGenerator");
                        Reflections.setAccessible(keyGeneratorField);
                        keyGeneratorField.set(UpdatedInfinispanUserSessionProviderFactory.this, keyGenerator);
                    } catch (IllegalAccessException iae) {
                        throw new RuntimeException(iae);
                    }

                    checkRemoteCaches(session);
                    loadPersistentSessions(factory, getMaxErrors(), getSessionsPerSegment());
                    registerClusterListeners(session);
                    loadSessionsFromRemoteCaches(session);

                } else if (event instanceof UserModel.UserRemovedEvent) {
                    UserModel.UserRemovedEvent userRemovedEvent = (UserModel.UserRemovedEvent) event;

                    InfinispanUserSessionProvider provider = (InfinispanUserSessionProvider) userRemovedEvent.getKeycloakSession().getProvider(UserSessionProvider.class, getId());
                    //provider.onUserRemoved(userRemovedEvent.getRealm(), userRemovedEvent.getUser());
                    // Need to use reflection :/
                    Method onUserRemoved = Reflections.findDeclaredMethod(InfinispanUserSessionProvider.class, "onUserRemoved", RealmModel.class, UserModel.class);
                    Reflections.setAccessible(onUserRemoved);
                    Reflections.invokeMethod(true, onUserRemoved, provider, userRemovedEvent.getRealm(), userRemovedEvent.getUser());
                }
            }
        });
    }

    // Max count of worker errors. Initialization will end with exception when this number is reached
    private int getMaxErrors() {
        return config.getInt("maxErrors", 20);
    }

    // Count of sessions to be computed in each segment
    private int getSessionsPerSegment() {
        return config.getInt("sessionsPerSegment", 100);
    }

    private void loadSessionsFromRemoteCaches(KeycloakSession session) {
        // Need to use reflection :/
        Field remoteCacheInvokerField = Reflections.findDeclaredField(InfinispanUserSessionProviderFactory.class, "remoteCacheInvoker");
        Reflections.setAccessible(remoteCacheInvokerField);
        RemoteCacheInvoker remoteCacheInvoker = Reflections.getFieldValue(remoteCacheInvokerField, this, RemoteCacheInvoker.class);

        for (String cacheName : remoteCacheInvoker.getRemoteCacheNames()) {
            loadSessionsFromRemoteCache(session.getKeycloakSessionFactory(), cacheName, getSessionsPerSegment(), getMaxErrors());
        }

    }


    private void loadSessionsFromRemoteCache(final KeycloakSessionFactory sessionFactory, String cacheName, final int sessionsPerSegment, final int maxErrors) {
        log.debugf("Check pre-loading sessions from remote cache '%s'", cacheName);

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
                Cache<String, Serializable> workCache = connections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

                InfinispanCacheInitializer initializer = new InfinispanCacheInitializer(sessionFactory, workCache, new UpdatedRemoteCacheSessionsLoader(cacheName), "remoteCacheLoad::" + cacheName, sessionsPerSegment, maxErrors);

                initializer.initCache();
                initializer.loadSessions();
            }

        });

        log.debugf("Pre-loading sessions from remote cache '%s' finished", cacheName);
    }


    @Override
    protected void checkRemoteCaches(KeycloakSession session) {
        super.checkRemoteCaches(session);

        replaceRemoteCacheListener(session, InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        replaceRemoteCacheListener(session, InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
        replaceRemoteCacheListener(session, InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
        replaceRemoteCacheListener(session, InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME);
        replaceRemoteCacheListener(session, InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME);
    }


    private void replaceRemoteCacheListener(KeycloakSession session, String cacheName) {
        InfinispanConnectionProvider ispn = session.getProvider(InfinispanConnectionProvider.class);
        Cache cache = ispn.getCache(cacheName);
        RemoteCache remoteCache = InfinispanUtil.getRemoteCache(cache);
        if (remoteCache != null) {
            log.debugf("Replacing clientListener for cache: %s", cacheName);

            Set<Object> listeners = remoteCache.getListeners();
            for (Object listener : listeners) {
                remoteCache.removeClientListener(listener);
            }

            UpdatedRemoteCacheSessionListener updatedListener = UpdatedRemoteCacheSessionListener.createListener(session, cache, remoteCache);
            remoteCache.addClientListener(updatedListener);

        }
    }

}
