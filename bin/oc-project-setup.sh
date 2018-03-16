#!/bin/bash -e

. `dirname $0`/load-config.sh

oc new-project $PROJECT

oc replace -n $PROJECT --force -f https://raw.githubusercontent.com/jboss-openshift/application-templates/ose-v1.4.9/sso/sso72-image-stream.json

oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default

oc secret new sso-jgroup-secret $SECRETS/certs/jgroups.jceks
oc secret new sso-ssl-secret $SECRETS/certs/keystore.jks $SECRETS/certs/truststore.jks

oc secrets link default sso-jgroup-secret sso-ssl-secret
