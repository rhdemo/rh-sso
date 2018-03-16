package org.keycloak.summit.infinispan.users;

import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;

/**
 * Needed on Wildfly due classloading.
 * @see org.keycloak.cluster.infinispan.KeycloakHotRodMarshallerFactory
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UpdatedKeycloakHotrodMarshallerFactory {

    public static GenericJBossMarshaller getInstance() {
        return new GenericJBossMarshaller(UpdatedKeycloakHotrodMarshallerFactory.class.getClassLoader());
    }

}
