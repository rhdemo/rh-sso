#!/bin/bash -e

cd `dirname $0`/..
. config

if [ ! -d .certs ]; then
    echo "Generating certs"
    mkdir .certs
    cd .certs

    keytool -genkey -alias localhost -keyalg RSA -keypass $CERT_PASS -storepass $CERT_PASS -keystore keystore.jks  -dname 'cn=localhost, ou=localhost, o=localhost, c=NO' -validity 300
    keytool -export -alias localhost -storepass $CERT_PASS -file sso.crt -keystore keystore.jks
    keytool -import -v -trustcacerts -alias localhost -file sso.crt -keystore truststore.jks -keypass $CERT_PASS -storepass $CERT_PASS -noprompt

    keytool -genseckey -alias jgroups -storetype JCEKS -keystore jgroups.jceks -keypass $CERT_PASS -storepass $CERT_PASS
else
    echo "Certificates already created"
fi
