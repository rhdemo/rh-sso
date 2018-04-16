package org.keycloak.summit.infinispan.rest;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;
import org.keycloak.summit.infinispan.users.JDGUserStorageProviderFactory;
import org.keycloak.utils.MediaType;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CacheMonitorResource {

    private final RemoteCache<Object, Object> remoteCache;

    private static final Logger log = Logger.getLogger(CacheMonitorResource.class);

    public CacheMonitorResource(KeycloakSession session, String cacheName) {
        // Workaround for the fact that userStorage cache is not defined in standalone-ha.xml as it's not in subsystem dependencies
        if (JDGUserStorageProviderFactory.CACHE_NAME.equals(cacheName)) {
            remoteCache = JDGUserStorageProviderFactory.getRemoteCache(session);
        } else {

            InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
            Cache cache = provider.getCache(cacheName);
            if (cache == null) {
                log.warn("Unknown cache: " + cacheName);
                throw new NotFoundException("Unknown cache: " + cacheName);
            }

            this.remoteCache = InfinispanUtil.getRemoteCache(cache);
            if (remoteCache == null) {
                log.warn("Remote cache not available: " + cacheName);
                throw new NotFoundException("Remote cache not available: " + cacheName);
            }
        }
    }


    @GET
    @Path("/contains/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean contains(@PathParam("id") String id) {
        return remoteCache.containsKey(id);
    }


    @GET
    @Path("/keys")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> enumerateKeys() {
        return remoteCache.keySet().stream().map((Object o) -> {

            return o.toString();

        }).collect(Collectors.toSet());
    }


    @GET
    @Path("/size")
    @Produces(MediaType.APPLICATION_JSON)
    public int size() {
        return remoteCache.size();
    }
}
