#!/bin/bash -e

. `dirname $0`/load-config.sh

oc delete --ignore-not-found=true all -l app=sso-single
oc delete --ignore-not-found=true pvc sso-mysql-claim
oc delete --ignore-not-found=true pvc sso-keycloak-providers-claim