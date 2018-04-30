SSO
===


Single cluster setup
--------------------

1. Login to oc
2. Checkout secrets repository in the same directory as rh-sso checkout (if you checkout to a different directory set
   SECRETS environment to point to it)
3. Run `bin/run-sso-single.sh`


All clusters setup working with JDG
-----------------------------------
On real servers (AWS, GCE, Azure), there is always project "datagrid" with deployed JDG server (more accurately
it is Infinispan server 9.2.0). Every cluster has the JDG available as a service. Any other pods, even from different 
projects (EG. RHSSO), can connect to it through Hotrod protocol on `jdg-app-hotrod.datagrid.svc:11222` .

Steps to deploy:
1. In the config file `../secretstuff/sso/config` , make sure that:
* Property `PROJECT` is set to some value, so it doesn't clash with other potentially existing projects 
on the cluster.
* Properties `AWS_LOGIN_CMD`, `AZR_LOGIN_CMD` and `GCE_LOGIN_CMD` are properly configured and contain oc login command
with your token. When you are logged through web UI into any cluster, you can click on your name in right top 
corner and then click `Copy Login Command` . Then copy/paste this into appropriate variable. 
* JDG related properties are configured like this (Explanation: We want JDG integration enabled. JDG host and port 
are available on all 3 clusters through the service with well-known name as described above. JDG_SITE is autodetected
according to cluster in which you are logged on, so it's not needed to configure this property):

```
export JDG_INTEGRATION_ENABLED=true
export JDG_HOST=jdg-app-hotrod.datagrid.svc
export JDG_PORT=11222
```

2. Run `bin/run-sso-all.sh`


Deploy SSO to work with JDG on localhost
----------------------------------------
If you want to test SSO with JDG on local openshift (either executed through `oc cluster up` or minishift), 
it's actually bit more complicated as you also need to provision JDG cluster locally. The steps are:
* Checkout `jdg-as-a-service` project and make sure to use `infinispan` branch. 
See the README file: https://github.com/rhdemo/jdg-as-a-service/blob/infinispan/README.asciidoc . You need 
to `simulate` 3 sites locally. I am using same openshift server and just 3 different projects like `infinispan`, `infinispan-2`, `infinispan-3`.

* In the `rh-sso` project, the configuration may look like this. Note that `stage` points to the site, where you deployed `infinispan` project
when you deployed JDG clusters in previous step. Site needs to be specified as the auto-detection won't work on the non-real servers:

```
export JDG_INTEGRATION_ENABLED=true
export JDG_SITE=Private
export JDG_HOST=jdg-app-hotrod.datagrid.svc
export JDG_PORT=11222
```
* On local network, the communication between JDG clusters won't work. At least for me, it doesn't work. It's due the fact that JGroups RELAY
stack in JDG servers is configured with some special JGroups protocols to work on real network, however it seems that working on local "simulated"
network doesn't work. This is not an issue. The most important is, if SSO server is able to connect to JDG server on stage site (project `infinispan`).
In real clusters environment, the communication between JDG servers through RELAY works and it's provided by `jdg-as-a-service` project.

Monitor JDG Remote cache
------------------------
We have some possibility to monitor JDG remote caches for debugging purposes. 
Assuming ROUTE is something like `https://openshift-master.summit-aws.sysdeseng.com`

Size of the specified cache
```
curl --insecure $ROUTE/auth/realms/summit/remote-cache/userStorage/size
```

Contains specified key:
```
curl --insecure $ROUTE/auth/realms/summit/remote-cache/userStorage/contains/id.john@email.cz
```

Keys of the specified cache (WARNING: Use with care if there is big amount of keys. 
It could theoretically cause OOM or such):
```
curl --insecure $ROUTE/auth/realms/summit/remote-cache/sessions/keys
```



  
   
