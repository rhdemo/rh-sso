# SSO

## Single cluster setup

1. Login to oc
2. Run `bin/create-self-signed-certs.sh` (this will create self-signed certificates and only needs to be executed once)
3. Run `bin/oc-project-setup.sh` (creates `sso` project and imports images, secrets, etc.)
4. Run `bin/oc-create-sso-single.sh` (creates `sso` application)
5. Run `bin/oc-config-sso.sh` (configures `sso`)

To delete and re-create you can run:

1. Run `bin/oc-delete-sso-single.sh`
2. Run `bin/oc-create-sso-single.sh` (creates `sso` application)
3. Run `bin/oc-project-setup.sh` (configures `sso`) 
