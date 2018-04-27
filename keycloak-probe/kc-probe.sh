#!/bin/bash

COUNT=10
SLEEP=5
SSO_PROBE_LOG=/tmp/rh-sso-probe.log

PYTHON_SCRIPT=$JBOSS_HOME/standalone/configuration/kc-send-request.py;
HOSTNAME=$(hostname);
#HOSTNAME="sso-sso-with-jdg.apps.summit-azr.sysdeseng.com";

if [ $# -gt 0 ] ; then
    COUNT=$1
fi

if [ $# -gt 1 ] ; then
    SLEEP=$2
fi

if [ $# -gt 2 ] ; then
    SSO_PROBE_LOG=$3
fi

# Fail-fast if EAP probe is failing
$JBOSS_HOME/bin/livenessProbe.sh $COUNT $SLEEP;
eapProbeStatus=$?;
if [ $eapProbeStatus -ne 0 ]; then
    exit $eapProbeStatus;
fi

echo "EAP liveness probe was OK";

function logMessage
{
    echo "$(date) - $1" >> $SSO_PROBE_LOG;
}

function sendRequest
{
    echo "Sending request";
    resp=$(python $PYTHON_SCRIPT $HOSTNAME);
    responseSuccess=$?;
    logMessage "$resp";
}

# Detect if JDG integration is enabled
echo $JAVA_OPTS_APPEND | grep remote.cache.host
if [ $? -eq 0 ]; then
    # Wait until request is sent successfully
    i=0;
    status=-1;
    sendRequest;
    echo "responseSuccess: $responseSuccess, i=$i";
    while [ $responseSuccess != 0 ] && [ $i -lt $COUNT ]; do
        i=$((i+1));
        echo "Waiting for $HOSTNAME to be ready. i=$i";
        sleep $SLEEP;
        sendRequest;
        echo "status: $status, i=$i";
    done;
else
    echo "JDG integration disabled. Ending with success";
    responseSuccess=0;
fi;

echo "Ending with $responseSuccess";
exit $responseSuccess;

