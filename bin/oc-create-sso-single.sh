#!/bin/bash -e

. `dirname $0`/load-config.sh

CA_CERT=`cat $SECRETS/certs/sso-rfc.crt`

detectJdgSite() {
    currentServer=$(oc whoami --show-server);
    echo "Logged to: $currentServer";

    if [ $currentServer == "https://openshift-master.summit-aws.sysdeseng.com:443" ]; then
        JDG_SITE=Amazon;
    elif [ $currentServer == "https://openshift-master.summit-gce.sysdeseng.com:443" ]; then
        JDG_SITE=Private;
    elif [ $currentServer == "https://openshift-master.summit-azr.sysdeseng.com:443" ]; then
        JDG_SITE=Azure;
    else
        echo "ERROR: JDG_SITE is not set and wasn't able to autodetect the site.";
        exit 1;
    fi;

    echo "Auto-detected JDG_SITE: $JDG_SITE";
}


if [ "$JDG_INTEGRATION_ENABLED" == "true" ]; then
    echo "JDG integration is enabled.";

    if [ "x$JDG_SITE" == "x" ]; then
        echo "JDG_SITE variable not set. Using autodetection";
        detectJdgSite;
    else
        echo "JDG_SITE pre-set in configuration to: $JDG_SITE";
    fi;

    # This assumes that image was build (oc-build-sso.sh was executed)
    JAVA_OPTS_APPEND="-Djboss.site.name=$JDG_SITE -Dremote.cache.host=$JDG_HOST -Dremote.cache.port=$JDG_PORT";
else
    echo "JDG integration is disabled.";
    JAVA_OPTS_APPEND="";
fi;

echo "JAVA_OPTS_APPEND: $JAVA_OPTS_APPEND";

oc new-app -f $DIR/sso-single.json \
-p IMAGE_STREAM_NAMESPACE=$PROJECT \
-p IMAGE_STREAM_NAME=sso72-jdg-image:latest \
-p APPLICATION_NAME=sso \
-p HTTPS_SECRET=sso-ssl-secret \
-p HTTPS_PASSWORD=$CERT_PASS \
-p JAVA_OPTS_APPEND="$JAVA_OPTS_APPEND" \
-p SSO_ADMIN_USERNAME=admin \
-p SSO_ADMIN_PASSWORD=$ADMIN_PASS \
-p SSO_TRUSTSTORE_SECRET=sso-ssl-secret \
-p SSO_TRUSTSTORE_PASSWORD=$CERT_PASS \
-p JGROUPS_ENCRYPT_SECRET=sso-jgroup-secret \
-p JGROUPS_ENCRYPT_PASSWORD=$CERT_PASS \
-p MEMORY_LIMIT=2Gi \
-p DB_MIN_POOL_SIZE=10 \
-p DB_MAX_POOL_SIZE=50 \
-p CA_CERTIFICATE="$CA_CERT"

# The production project namespace is 'sso', but in some cases, we deploy SSO in other namespaces for testing/debugging purposes.
# For example 'sso-with-jdg' for performance testing. In this case, we need to remove secure-sso-redhatkeynote route to not clash
# with main 'sso' namespace
if [ "$PROJECT" != "sso" ]; then
   echo "We are not in project 'sso' . Deleting route secure-sso-redhatkeynote"
   oc delete route/secure-sso-redhatkeynote
fi;
