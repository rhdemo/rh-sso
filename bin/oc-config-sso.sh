#!/bin/bash -e

cd `dirname $0`/..
. bin/config

CONFIG=$PWD/.keycloak/kcadmin.config
SERVER=http://`oc get routes sso -o jsonpath='{.spec.host}'`/auth


POD=`oc get pods -l deployment=sso-1 -o jsonpath='{.items[0].metadata.name}'`

mvn clean install
oc cp keycloak-providers/keycloak-providers-ear/target/keycloak-summit-providers.ear $POD:/opt/eap/standalone/deployments/
oc exec $POD touch /opt/eap/standalone/deployments/keycloak-summit-providers.ear.dodeploy

# TODO Use https
#bin/kcadm.sh config truststore --config $CONFIG --trustpass $CERT_PASS $PWD/.certs/truststore.jks

bin/kcadm.sh config credentials --config $CONFIG --server $SERVER --realm master --user admin --password $ADMIN_PASS
bin/kcadm.sh create realms --config $CONFIG -f realm-summit.json