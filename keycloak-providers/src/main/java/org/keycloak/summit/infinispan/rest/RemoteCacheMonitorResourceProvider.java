package org.keycloak.summit.infinispan.rest;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RemoteCacheMonitorResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public RemoteCacheMonitorResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    public Object getResource() {
        return this;
    }

    @Path("/{cache}")
    public CacheMonitorResource getCacheResource(@PathParam("cache") String cacheName) {
        return new CacheMonitorResource(session, cacheName);
    }

    @Override
    public void close() {

    }
}
