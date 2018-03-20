#!/bin/bash -e

oc delete --ignore-not-found=true all -l app=sso-single
oc delete --ignore-not-found=true pvc sso-mysql-claim
oc delete --ignore-not-found=true pvc sso-keycloak-providers-claim