#!/bin/bash -e

. `dirname $0`/load-config.sh

echo "----------------------------------"
echo "Configuring SSO in project '$PROJECT' on server '$(oc whoami --show-server)' as user '$(oc whoami)'";
echo "----------------------------------"

TMP=`mktemp -d`
TMP_CONFIG=$TMP/kcadmin-config
TMP_CERT=$TMP/cert
TMP_KEYSTORE=$TMP/keystore

SERVER=https://`oc get routes secure-sso -o jsonpath='{.spec.host}'`/auth

# Wait until container is ready
while [ `curl -s -o /dev/null -w "%{http_code}" -k $SERVER/realms/master/` != 200 ]; do
    echo "Waiting for $SERVER to be ready";
    sleep 10;
done

# Create truststore with cert

HOST=`oc get route secure-sso -o jsonpath='{.spec.host}'`

openssl s_client -connect $HOST:443 </dev/null | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > $TMP_CERT

keytool -import -noprompt -trustcacerts -alias $HOST -file $TMP_CERT -keystore $TMP_KEYSTORE -storepass $CERT_PASS

bin/kcadm.sh config truststore --config $CONFIG --trustpass $CERT_PASS $TMP_KEYSTORE

bin/kcadm.sh config credentials --config $CONFIG --server $SERVER --realm master --user admin --password $ADMIN_PASS

bin/kcadm.sh create realms --config $CONFIG -f realm-summit.json \
-s smtpServer.from=$SMTP_FROM \
-s smtpServer.user=$SMTP_USER \
-s smtpServer.password=$SMTP_PASSWORD

bin/kcadm.sh update --config $CONFIG -r summit identity-provider/instances/google -s config.clientId=$GOOGLE_CLIENT_ID -s config.clientSecret=$GOOGLE_CLIENT_SECRET
bin/kcadm.sh update --config $CONFIG -r summit identity-provider/instances/developers -s config.clientId=$DEVELOPERS_CLIENT_ID -s config.clientSecret=$DEVELOPERS_CLIENT_SECRET

bin/kcadm.sh update --config $CONFIG -r summit clients/e0810662-2a8d-46ae-9419-40718b5fa3e0 -s redirectUris=$GAME_CLIENT_REDIRECT_URIS -s rootUrl=$GAME_CLIENT_ROOT_URL

rm -rf $TMP
