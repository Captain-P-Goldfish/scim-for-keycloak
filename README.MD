# What is SCIM-for-keycloak?

It is a full fleshed SCIM implementation that is easily integrated into keycloak and is also easily configurable within
the web admin console. You'll be able to restrict access of endpoints with roles and to specific clients . Select the
option `scim` on entry `Admin Console Theme` and reload the page (Press F5).

## Keycloak support matrix

|  version | supported          | scim-for-keycloak version  |
| :------- | :----------------- | :------------------------- |
| lower    | :x:                | -                          |
| 9.0.x    | :x:                | You may find an older working version at https://github.com/Captain-P-Goldfish/SCIM-SDK |     
| 10.0.x   | :x:                | You may find an older working version at https://github.com/Captain-P-Goldfish/SCIM-SDK |     
| 11.0.x   | :heavy_check_mark: | kc-11.b${buildnumber}      |     

## Versioning

This project will use the following versioning pattern

```kc-${keycloak.version}-b${project.incremental.version}```

## Releases

Releases will be only tagged in git and not be uploaded to any repository. Please build the necessary artifacts with the
following description yourself.

## Deploy into a standalone keycloak server?

Switch into the parent directory "scim-for-keycloak" and build the project once with `mvn package -DskipTests`. Copy the
file `scim-for-keycloak/scim-for-keycloak-deployment/target/scim-for-keycloak-kc-${keycloak.version}-b${project.incremental.version}.ear`
into the directory `${keycloak.home}/standalone/deployments` of a keycloak server.  
**NOTE:** *If the server is running during deployment you need to restart it!*

## How do I configure the SCIM endpoints?

1. Deploy the `scim-for-keycloak.ear` file into your server. If the server is currently running you need to restart it!
2. Now open the web admin console of keycloak open the realm settings and select the `Themes` tab and select the `scim`
   theme for the admin console. Now reload the browser page and the `SCIM` menu link should be visible.
   ![theme-settings](images/theme-setting.png)

## What is configurable?

1. Select the `SCIM` link and you should see the following view:   
   ![theme-settings](images/service-provider-config.png)
2. You can disable SCIM for each realm separately and the `ServiceProvider` configurations as defined in RFC7643
3. You may restrict access to the SCIM-endpoints for each separate realm by allowing users only access if they have been
   identified by a specific client. If no clients are selected any client is authorized to access the SCIM endpoints:  
   ![theme-settings](images/service-provider-auth-config.png)
4. You get an overview for all registered `ResourceTypes` and may configure them separately.  
   ![theme-settings](images/resource-type-overview.png)  
   You may have noticed that a 3rd `ResourceType` with name `RealmRole` is present that is not defined by RFC7643. This
   `ResourceType` is a custom endpoint that acts as showcase to demonstrate how easy it is to extend the API with new
   endpoints. This endpoint provides support for adding new `RealmRoles` and associate them with either `Groups` or
   `Users`.
5. Each endpoint provides several settings that can be configured:  
   ![theme-settings](images/resource-type-config.png)  
   You'll be able to change the description of an endpoint, disable a specific `ResourceType` or just prevent that new
   resources are created, deleted, updated or read.
6. You can turn off the requirement for authentication on each `ResourceType` but not for specific
   `ResourceType`-endpoints.
7. You'll be able to set required roles for each `ResourceType` and each `ResourceType`
   -endpoint [create, get , list, update, delete]  
   ![theme-settings](images/resource-type-auth-config.png)  
   The role settings are of course only respected if authentication is enabled.

## Disadvantages

1. I didn't find a really good way in storing multi-valued-complex structures in the user-attributes table so I stored
   such data structures in pure json in this table. Please note that the values in the screenshot are simply
   auto-generated test-values.
   ![theme-settings](images/user-storage.png)
2. User loading and filtering is not implemented on database level meaning that a "list-Users"-request will currently
   retrieve all Users of the used realm from the database. Filtering, sorting and limiting of the returned entries will
   be done on API level by the SCIM-SDK. (personal tests have shown that this is still pretty fast with at least 5000
   Users. I never tried more)

## Grant management-access for configuration

In some cases you might want to have several administration users that will only be able to configure the SCIM
environment of a single realm. In such a case the user must have specific roles that will grant access to configure the
SCIM environment. If the user does not have the corresponding role the "SCIM"-menu entry will not be visible to this
user.

Two different roles are required to access the "SCIM"-menu.

1. view-realm (allows the user to see the "SCIM"-menu item on the left)
2. scim-admin (allows the user to modify the SCIM configuration)

The "scim-admin" role exists several times for several clients.

1. For each realm that is created a new "master-client" for this realm is added to the "master" realm. Each of these
   clients will get the "scim-admin" role that is needed for users from the "master" realm to access the "SCIM"-menu. So
   if you create a realm with name "test" you will be able to find a client with name "test-realm" within the clients
   section of the "master" realm. The "scim-admin" role of this client is needed to modify the data of the SCIM
   environment in the "test" realm for users of realm "master".

2. If a new realm "test" is created you should be able to find a client in the clients section of realm "test" with name
   "realm-management". This client should have the client-role "scim-admin". Grant this role to users of realm
   "test" to grant access to the SCIM configuration for this realm.

3. The last case is the SCIM configuration for realm "master". This works analogous to 2. But for realm "master" the
   name of the "master-client" is "master-realm" instead of "realm-management".

So in total we got 2 different cases:

1. users of realm "master" are able to get access to SCIM configurations of several realms.
2. users of other realms can only access the SCIM configuration of their own realm.

![theme-settings](images/scim-access-from-master-realm-to-master.png)  
![theme-settings](images/scim-access-for-realm-test.png)  
![theme-settings](images/scim-access-from-test-realm-to-test.png)  
