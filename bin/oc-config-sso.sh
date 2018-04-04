#!/bin/bash -e

cd `dirname $0`/..
. config

TMP=`mktemp -d`
TMP_CONFIG=$TMP/kcadmin-config
TMP_CERT=$TMP/cert
TMP_KEYSTORE=$TMP/keystore

SERVER=https://`oc get routes secure-sso -o jsonpath='{.spec.host}'`/auth

# Wait until container is ready
while [ `curl -s -o /dev/null -w "%{http_code}" -k $SERVER` != 200 ]; do
    echo "Waiting for $SERVER to be ready";
    sleep 10;
done

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
