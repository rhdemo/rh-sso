package org.keycloak.summit.infinispan;

import java.lang.reflect.Field;
import java.util.Random;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.remotestore.ClientListenerExecutorDecorator;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionListener;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UpdatedRemoteCacheSessionListener<K, V extends SessionEntity> extends RemoteCacheSessionListener<K, V> {

    protected static final Logger logger = Logger.getLogger(UpdatedRemoteCacheSessionListener.class);

    protected UpdatedRemoteCacheSessionListener() {
    }


    public static <K, V extends SessionEntity> UpdatedRemoteCacheSessionListener createListener(KeycloakSession session, Cache<K, SessionEntityWrapper<V>> cache, RemoteCache<K, SessionEntityWrapper<V>> remoteCache) {
        UpdatedRemoteCacheSessionListener<K, V> listener = new UpdatedRemoteCacheSessionListener<>();
        listener.init(session, cache, remoteCache);

        return listener;
    }


    @Override
    @ClientCacheEntryCreated
    public void created(ClientCacheEntryCreatedEvent event) {
        K key = (K) event.getKey();

        if (shouldUpdateLocalCache(event.getType(), key, event.isCommandRetried())) {
            ClientListenerExecutorDecorator executor = getFieldValue("executor", ClientListenerExecutorDecorator.class);
            executor.submit(event, () -> {

                // Should load it from remoteStore - NO (With infinispan server 9.2 it doesn't work because remoteCache.getWithMetadata always return metadata with lastUsed set
                // incorrectly to 0. This causes PersistenceUtil.loadAndCheckExpiration to always return null (due the InternalMetada.isExpired is true). Not sure if
                // it's infinispan bug TODO Possibly more investigation or revert to cache.get after upgrading keycloak infinispan version to 9.X (hopefully this will fix it)
                //cache.get(key);
                createRemoteEntityInCache(key, event.getVersion());

            });
        }
    }


    // Reflection hack...
    protected <F> F getFieldValue(String fieldName, Class<F> expectedClass) {
        try {
            Field f = RemoteCacheSessionListener.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return Reflections.getFieldValue(f, this, expectedClass);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        }
    }


    protected void createRemoteEntityInCache(K key, long eventVersion) {
        boolean finished = false;
        int createRetries = 0;
        int sleepInterval = 25;

        Cache<K, SessionEntityWrapper<V>> cache = getFieldValue("cache", Cache.class);
        RemoteCache<K, SessionEntityWrapper<V>> remoteCache = getFieldValue("remoteCache", RemoteCache.class);
        do {
            createRetries++;

            VersionedValue<SessionEntityWrapper<V>> remoteSessionVersioned = remoteCache.getVersioned(key);

            // Maybe can happen under some circumstances that remoteCache doesn't yet contain the value sent in the event (maybe just theoretically...)
            if (remoteSessionVersioned == null || remoteSessionVersioned.getValue() == null) {
                try {
                    logger.debugf("Entity '%s' not yet present in remoteCache. Postponing create",
                            key.toString());
                    Thread.sleep(new Random().nextInt(sleepInterval));  // using exponential backoff
                    continue;
                } catch (InterruptedException ex) {
                    continue;
                } finally {
                    sleepInterval = sleepInterval << 1;
                }
            }


            V remoteSession = remoteSessionVersioned.getValue().getEntity();
            SessionEntityWrapper<V> newWrapper = new SessionEntityWrapper<>(remoteSession);

            logger.debugf("Read session entity wrapper from the remote cache: %s . createRetries=%d", remoteSession.toString(), createRetries);

            // Using putIfAbsent. Theoretic possibility that entity was already put to cache by someone else
            cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_STORE, Flag.SKIP_CACHE_LOAD, Flag.IGNORE_RETURN_VALUES)
                    .putIfAbsent(key, newWrapper);

            finished = true;

        } while (createRetries < 10 && ! finished);

    }


    @Override
    @ClientCacheEntryModified
    public void updated(ClientCacheEntryModifiedEvent event) {
        super.updated(event);
    }

    @Override
    @ClientCacheEntryRemoved
    public void removed(ClientCacheEntryRemovedEvent event) {
        super.removed(event);
    }
}
