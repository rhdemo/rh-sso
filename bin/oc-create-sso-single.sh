#!/bin/bash -e

cd `dirname $0`/..
. config

oc new-app -f sso-single.json \
-p IMAGE_STREAM_NAMESPACE=sso \
-p APPLICATION_NAME=sso \
-p HTTPS_SECRET=sso-ssl-secret \
-p HTTPS_PASSWORD=$CERT_PASS \
-p SSO_ADMIN_USERNAME=admin \
-p SSO_ADMIN_PASSWORD=$ADMIN_PASS \
-p SSO_TRUSTSTORE_SECRET=sso-ssl-secret \
-p SSO_TRUSTSTORE_PASSWORD=$CERT_PASS \
-p JGROUPS_ENCRYPT_SECRET=sso-jgroup-secret \
-p JGROUPS_ENCRYPT_PASSWORD=$CERT_PASS \
-p MEMORY_LIMIT=2Gi