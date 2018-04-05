#!/bin/bash -e

. `dirname $0`/load-config.sh

if (! oc get project $PROJECT 2>/dev/null | grep Active &>/dev/null); then
    echo "----------------------------------"
    echo "Project not found, creating"
    echo "----------------------------------"
    bin/oc-project-setup.sh
fi

echo "----------------------------------"
echo "Deleting old version if it exists"
echo "----------------------------------"

$DIR/bin/oc-delete-sso-single.sh

echo "----------------------------------"
echo "Building SSO image"
echo "----------------------------------"

$DIR/bin/oc-build-sso.sh

echo "----------------------------------"
echo "Creating SSO"
echo "----------------------------------"

$DIR/bin/oc-create-sso-single.sh

echo "----------------------------------"
echo "Configuring SSO"
echo "----------------------------------"

$DIR/bin/oc-config-sso.sh