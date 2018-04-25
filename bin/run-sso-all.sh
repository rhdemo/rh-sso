#!/bin/bash -e

. `dirname $0`/load-config.sh

function ocLogin
{
    echo "Login to $2 now";

    if [ -z "$1" ]; then
        echo "Variable $2_LOGIN_CONFIG not defined. Check your config";
        exit 1;
    fi;

    eval $1;
}

# First create project on all clusters. Then configure it. This is to allow concurrent start of clusters
ocLogin "$AWS_LOGIN_CMD" "AWS";
$DIR/bin/run-sso-parent.sh;

ocLogin "$AZR_LOGIN_CMD" "AZR";
$DIR/bin/run-sso-parent.sh;

ocLogin "$GCE_LOGIN_CMD" "GCE";
$DIR/bin/run-sso-parent.sh;

ocLogin "$AWS_LOGIN_CMD" "AWS";
$DIR/bin/oc-config-sso.sh;

ocLogin "$AZR_LOGIN_CMD" "AZR";
$DIR/bin/oc-config-sso.sh;

ocLogin "$GCE_LOGIN_CMD" "GCE";
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



