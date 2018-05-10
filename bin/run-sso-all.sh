#!/bin/bash -e

. `dirname $0`/load-config.sh

function ocLogin
{
    echo "Login to $1 with token $2 now";

    if [ -z "$1" ]; then
        echo "Required variable $3 not defined. Check your config";
        exit 1;
    fi;
    if [ -z "$2" ]; then
        echo "Required variable $4 not defined. Check your config";
        exit 1;
    fi;

    oc login $1 --token=$2
}

# First create project on all clusters. Then configure it. This is to allow concurrent start of clusters
ocLogin "$AWS_OPENSHIFT_URL" "$AWS_TOKEN" "AWS_OPENSHIFT_URL" "AWS_TOKEN";
$DIR/bin/run-sso-parent.sh;

ocLogin "$AZR_OPENSHIFT_URL" "$AZR_TOKEN" "AZR_OPENSHIFT_URL" "AZR_TOKEN";
$DIR/bin/run-sso-parent.sh;

ocLogin "$GCE_OPENSHIFT_URL" "$GCE_TOKEN" "GCE_OPENSHIFT_URL" "GCE_TOKEN";
$DIR/bin/run-sso-parent.sh;

ocLogin "$AWS_OPENSHIFT_URL" "$AWS_TOKEN" "AWS_OPENSHIFT_URL" "AWS_TOKEN";
$DIR/bin/oc-config-sso.sh;

ocLogin "$AZR_OPENSHIFT_URL" "$AZR_TOKEN" "AZR_OPENSHIFT_URL" "AZR_TOKEN";
$DIR/bin/oc-config-sso.sh;

ocLogin "$GCE_OPENSHIFT_URL" "$GCE_TOKEN" "GCE_OPENSHIFT_URL" "GCE_TOKEN";
$DIR/bin/oc-config-sso.sh;

if [ $JDG_INTEGRATION_ENABLED == "true" ]; then
    echo "----------------------------------"
    echo "Executing the JDG integration test";
    echo "----------------------------------"
    cd $DIR
    mvn clean install -Pjdg-test
else
    echo "JDG Integration is disabled. Skip running test";
fi;



