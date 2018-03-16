package org.keycloak.summit.infinispan;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionsLoader;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UpdatedRemoteCacheSessionsLoader extends RemoteCacheSessionsLoader {

    private static final Logger log = Logger.getLogger(UpdatedRemoteCacheSessionsLoader.class);

    // Javascript to be executed on remote infinispan server.
    // Flag CACHE_MODE_LOCAL is optimization used just when remoteCache is replicated as all the entries are available locally. For distributed caches, it can't be used
    // encodingClazz is needed on Infinispan Server 9.2.0
    private static final String REMOTE_SCRIPT_FOR_LOAD_SESSIONS =
            "function loadSessions() {" +
                    "  var flagClazz = cache.getClass().getClassLoader().loadClass(\"org.infinispan.context.Flag\"); \n" +
                    "  var localFlag = java.lang.Enum.valueOf(flagClazz, \"CACHE_MODE_LOCAL\"); \n" +
                    "  var cacheMode = cache.getCacheConfiguration().clustering().cacheMode(); \n" +
                    "  var canUseLocalFlag = !cacheMode.isClustered() || cacheMode.isReplicated(); \n" +
                    "  var encodingClazz = Java.type(\"org.infinispan.commons.dataconversion.IdentityEncoder\").class; \n" +

                    "  var cacheStream; \n" +
                    "  if (canUseLocalFlag) { \n" +
                    "      cacheStream = cache.getAdvancedCache().withEncoding(encodingClazz).withFlags([ localFlag ]).entrySet().stream();\n" +
                    "  } else { \n" +
                    "      cacheStream = cache.getAdvancedCache().withEncoding(encodingClazz).withFlags([ ]).entrySet().stream();\n" +
                    "  }; \n" +

                    "  var result = cacheStream.skip(first).limit(max).collect(java.util.stream.Collectors.toMap(\n" +
                    "    new java.util.function.Function() {\n" +
                    "      apply: function(entry) {\n" +
                    "        return entry.getKey();\n" +
                    "      }\n" +
                    "    },\n" +
                    "    new java.util.function.Function() {\n" +
                    "      apply: function(entry) {\n" +
                    "        return entry.getValue();\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ));\n" +
                    "\n" +
                    "  cacheStream.close();\n" +
                    "  return result;\n" +
                    "};\n" +
                    "\n" +
                    "loadSessions();";


    private final String cacheName;

    public UpdatedRemoteCacheSessionsLoader(String cacheName) {
        super(cacheName);
        this.cacheName = cacheName;
    }


    @Override
    public void init(KeycloakSession session) {
        RemoteCache remoteCache = InfinispanUtil.getRemoteCache(getCache(session));

        RemoteCache<String, String> scriptCache = remoteCache.getRemoteCacheManager().getCache("___script_cache");

        if (!scriptCache.containsKey("load-sessions-updated.js")) {
            scriptCache.put("load-sessions-updated.js",
                    "// mode=local,language=javascript\n" +
                            REMOTE_SCRIPT_FOR_LOAD_SESSIONS);
        }
    }


    @Override
    public boolean loadSessions(KeycloakSession session, int first, int max) {
        Cache cache = getCache(session);
        Cache decoratedCache = cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_CACHE_STORE, Flag.IGNORE_RETURN_VALUES);

        RemoteCache<?, ?> remoteCache = InfinispanUtil.getRemoteCache(cache);

        log.debugf("Will do bulk load of sessions from remote cache '%s' . First: %d, max: %d", cache.getName(), first, max);

        Map<String, Integer> remoteParams = new HashMap<>();
        remoteParams.put("first", first);
        remoteParams.put("max", max);
        Map<byte[], byte[]> remoteObjects = remoteCache.execute("load-sessions-updated.js", remoteParams);

        log.debugf("Successfully finished loading sessions '%s' . First: %d, max: %d", cache.getName(), first, max);

        Marshaller marshaller = remoteCache.getRemoteCacheManager().getMarshaller();

        for (Map.Entry<byte[], byte[]> entry : remoteObjects.entrySet()) {
            try {
                Object key = marshaller.objectFromByteBuffer(entry.getKey());
                SessionEntityWrapper entityWrapper = (SessionEntityWrapper) marshaller.objectFromByteBuffer(entry.getValue());

                decoratedCache.putAsync(key, entityWrapper);
            } catch (Exception e) {
                log.warn("Error loading session from remote cache", e);
            }
        }

        return true;
    }


    private Cache getCache(KeycloakSession session) {
        InfinispanConnectionProvider ispn = session.getProvider(InfinispanConnectionProvider.class);
        return ispn.getCache(cacheName);
    }
}
