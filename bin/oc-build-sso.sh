#!/bin/bash -e

cd `dirname $0`/..
. config

mvn clean install
rm -rf target
mkdir target
mkdir target/configuration

if [ "$JDG_INTEGRATION_ENABLED" == "true" ]; then
    echo "JDG integration is enabled. Using custom file standalone-openshift-cfg/configuration/standalone-openshift-jdg.xml";
    cp standalone-openshift-cfg/configuration/standalone-openshift-jdg.xml target/configuration/standalone-openshift.xml
else
    echo "JDG integration is disabled. Using custom file standalone-openshift-cfg/configuration/standalone-openshift-nojdg.xml";
    cp standalone-openshift-cfg/configuration/standalone-openshift-nojdg.xml target/configuration/standalone-openshift.xml
fi;

mkdir target/deployments
cp keycloak-providers/keycloak-providers-ear/target/keycloak-summit-providers.ear target/deployments
touch target/deployments/keycloak-summit-providers.ear.dodeploy
echo "Prepared directory 'target' to be used for custom build"

oc delete imagestream/sso72-jdg-image --ignore-not-found=true
oc delete buildconfig/sso72-jdg-image --ignore-not-found=true

echo "Building the image: sso72-jdg-image.";
oc new-build --binary=true --name=sso72-jdg-image --image-stream=redhat-sso72-openshift:1.0
oc start-build sso72-jdg-image --from-dir=./target --follow

