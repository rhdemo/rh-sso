#!/bin/bash -e

cd `dirname $0`/..
. config

TMP=`mktemp -d`
TMP_CONFIG=$TMP/kcadmin-config
TMP_CERT=$TMP/cert
TMP_KEYSTORE=$TMP/keystore

SERVER=https://`oc get routes secure-sso -o jsonpath='{.spec.host}'`/auth

POD=`oc get pods -l deployment=sso-1 -o jsonpath='{.items[0].metadata.name}'`

# Wait until container is ready
READY=false;
while [  "$READY" == "false" ]; do
    READY=`oc get pod $POD -o jsonpath='{.status.containerStatuses[0].ready}'`

    if [ "$READY" == "false" ]; then
        echo "Pod $POD not yet ready. Waiting..."
        sleep 5;
    fi;
done

echo "Pod $POD is ready! Continue configuring sso pod $POD";
sleep 5; # Rather sleep until route is finished

# TODO Use https

# Create truststore with cert

HOST=`oc get route secure-sso -o jsonpath='{.spec.host}'`

openssl s_client --connect $HOST:443 </dev/null | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > cert > $TMP_CERT

keytool -import -noprompt -trustcacerts -alias $HOST -file $TMP_CERT -keystore $TMP_KEYSTORE -storepass $CERT_PASS

bin/kcadm.sh config truststore --config $CONFIG --trustpass $CERT_PASS $TMP_KEYSTORE

bin/kcadm.sh config credentials --config $CONFIG --server $SERVER --realm master --user admin --password $ADMIN_PASS

bin/kcadm.sh create realms --config $CONFIG -f realm-summit.json \
-s smtpServer.from=$SMTP_FROM \
-s smtpServer.user=$SMTP_USER \
-s smtpServer.password=$SMTP_PASSWORD

bin/kcadm.sh update --config $CONFIG -r summit identity-provider/instances/google -s config.clientId=$GOOGLE_CLIENT_ID -s config.clientSecret=$GOOGLE_CLIENT_SECRET
bin/kcadm.sh update --config $CONFIG -r summit identity-provider/instances/developers -s config.clientId=$DEVELOPERS_CLIENT_ID -s config.clientSecret=$DEVELOPERS_CLIENT_SECRET

rm -rf $TMP
