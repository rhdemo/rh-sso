#!/bin/bash -e

cd `dirname $0`/..
. bin/config

oc new-project $PROJECT

oc replace -n $PROJECT --force -f https://raw.githubusercontent.com/jboss-openshift/application-templates/ose-v1.4.9/sso/sso72-image-stream.json

oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default

cd .certs
oc secret new sso-jgroup-secret jgroups.jceks
oc secret new sso-ssl-secret keystore.jks truststore.jks
cd ..

oc secrets link default sso-jgroup-secret sso-ssl-secret
