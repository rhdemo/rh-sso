# SSO

## Single cluster setup

1. Login to oc
2. Create file `config` from template in file `bin/config-template`


    $ cp bin/config-template config
    $ chmod u+x config 
         
3. Configure things in file `/bin/config` (SMTP server, Google client, RH developers client etc)
4. Copy admin client binaries from RHSSO 7.2 distribution as script `kcadm.sh` needs them. 


    $ cp -r $RHSSO72_HOME/bin/client bin/
    
5. Run `bin/create-self-signed-certs.sh` (this will create self-signed certificates and only needs to be executed once)
6. Run `bin/oc-project-setup.sh` (creates `sso` project and imports images, secrets, etc.)
7. Run `bin/oc-create-sso-single.sh` (creates `sso` application)
8. Run `bin/oc-config-sso.sh` (configures `sso`)

To delete and re-create you can run:

1. Run `bin/oc-delete-sso-single.sh`
2. Run `bin/oc-create-sso-single.sh` (creates `sso` application)
3. Run `bin/oc-config-sso.sh` (configures `sso`) 
