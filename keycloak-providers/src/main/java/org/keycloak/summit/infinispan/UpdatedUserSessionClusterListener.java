package org.keycloak.summit.infinispan;

import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProvider;
import org.keycloak.models.sessions.infinispan.events.AbstractUserSessionClusterListener;
import org.keycloak.models.sessions.infinispan.events.SessionClusterEvent;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class UpdatedUserSessionClusterListener<SE extends SessionClusterEvent> implements ClusterListener {

    private static final Logger log = Logger.getLogger(AbstractUserSessionClusterListener.class);

    private final KeycloakSessionFactory sessionFactory;

    public UpdatedUserSessionClusterListener(KeycloakSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }


    @Override
    public void eventReceived(ClusterEvent event) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, (KeycloakSession session) -> {
            InfinispanUserSessionProvider provider = (InfinispanUserSessionProvider) session.getProvider(UserSessionProvider.class);
            SE sessionEvent = (SE) event;

            boolean shouldResendEvent = shouldResendEvent(session, sessionEvent);

            if (log.isDebugEnabled()) {
                log.debugf("Received user session event '%s'. Should resend event: %b", sessionEvent.toString(), shouldResendEvent);
            }

            eventReceived(session, provider, sessionEvent);

            if (shouldResendEvent) {
                session.getProvider(ClusterProvider.class).notify(sessionEvent.getEventKey(), event, true, ClusterProvider.DCNotify.ALL_BUT_LOCAL_DC);
            }

        });
    }

    protected abstract void eventReceived(KeycloakSession session, InfinispanUserSessionProvider provider, SE sessionEvent);


    private boolean shouldResendEvent(KeycloakSession session, SessionClusterEvent event) {
        if (!event.isResendingEvent()) {
            return false;
        }

        // Just the initiator will re-send the event after receiving it
        String myNode = InfinispanUtil.getMyAddress(session);
        String mySite = InfinispanUtil.getMySite(session);
        return (event.getNodeId() != null && event.getNodeId().equals(myNode) && event.getSiteId() != null && event.getSiteId().equals(mySite));
    }
}
