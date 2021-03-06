|  Branch | Circle CI Status |
| :------ | :--------------- | 
| master  | [![CircleCI](https://circleci.com/gh/Captain-P-Goldfish/scim-for-keycloak/tree/master.svg?style=shield)](https://circleci.com/gh/Captain-P-Goldfish/scim-for-keycloak/tree/master)

# What is SCIM-for-keycloak?

It is a full SCIM implementation that is easily integrated into keycloak and is also easily configurable within the web
admin console.

It grants you interoperable User, Group and Role provisioning by using a defined standard for resource managing:

See:

* https://tools.ietf.org/html/rfc7643
* https://tools.ietf.org/html/rfc7644

An example is user and group synchronizing with Microsoft Azure AD. A manual that describes how to do this can be found
in the wiki:

[Use with Microsoft Azure AD](https://github.com/Captain-P-Goldfish/scim-for-keycloak/wiki/Use-with-Microsoft-Azure-AD)
Thanks to *Raffael Grob* for providing this manual.

## Keycloak support matrix

|  version | supported          | scim-for-keycloak version  | SCIM-SDK Version |
| :------- | :----------------- | :------------------------- | ---------------- |
| lower    | :x:                | -                          |                  |
| 9.0.x    | :x:                | You may find an older working version at https://github.com/Captain-P-Goldfish/SCIM-SDK |   | 
| 10.0.x   | :x:                | You may find an older working version at https://github.com/Captain-P-Goldfish/SCIM-SDK |   |  
| 11.0.x   | :heavy_check_mark: | kc-11-b1                   | 1.8.3            |
| 12.0.x   | :heavy_check_mark: | kc-12-b1                   | 1.8.3            |
| 12.0.x   | :heavy_check_mark: | kc-12-b2                   | 1.9.2            |
| 13.0.0   | :heavy_check_mark: | kc-13-b1                   | 1.9.2            |
| 13.0.1   | :heavy_check_mark: | kc-13-b2                   | 1.11.0           |
| 13.0.1   | :heavy_check_mark: | kc-13-b3                   | 1.11.0           |
| 14.0.0   | :heavy_check_mark: | kc-14-b1                   | 1.11.0           |
| 14.0.0   | :heavy_check_mark: | kc-14-b2                   | 1.11.0           |

## Versioning

This project will use the following versioning pattern

```kc-${keycloak.version}-b${project.incremental.version}```

## Releases

Releases will be only tagged in git and not be uploaded to any repository. Please build the necessary artifacts with the
following description yourself.

## How to build

You need a git, JDK and maven installation on your computer

Checkout the git tag that you want to build:

1. list all available tags:  `git tag -l *`
2. checkout the tag you want to build `git checkout tags/${tag-name}`
3. build with maven
   * `cd ${SCIM_FOR_KEYCLOAK_HOME}`
   * `mvn clean package -DskipTests`
4. find the scim-for-keycloak artifact in:
   `scim-for-keycloak/scim-for-keycloak-deployment/target/scim-for-keycloak-kc-${keycloak.version}-b${project.incremental.version}.ear`

## Deploy into a standalone keycloak server

Copy the built artifact into the directory `${keycloak.home}/standalone/deployments` of a keycloak server.  
**NOTE:** *If the server is running during deployment you need to restart it!*

## How to configure the SCIM endpoints

1. Deploy the `scim-for-keycloak.ear` file into your server. If the server is currently running you need to restart it!
2. Now open the web admin console of keycloak open the realm settings and select the `Themes` tab and select the `scim`
   theme for the admin console. Now reload the browser page and the `SCIM` menu link should be visible.
   ![theme-settings](images/theme-setting.png)
3. The SCIM endpoints will be `deactivated by default`. To activate it for a realm toggle the "SCIM enabled" switch
   under the SCIM menu link.

## What is configurable?

1. Select the `SCIM` link and you should see the following view:   
   ![theme-settings](images/service-provider-config.png)
2. You can enable SCIM for each realm separately and the `ServiceProvider` configurations as defined in RFC7643
3. You may restrict access to the SCIM-endpoints for each separate realm by allowing users only access if they have been
   identified by a specific client.
   `If no clients are selected any client is authorized to access the SCIM endpoints`  
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
7. You'll be able to set required roles for each `ResourceType` and each `ResourceType`-endpoint
   [create, get , list, update, delete]
   . `If no roles are applied to an endpoint the API will assume the authorization to be disabled.`
   ![theme-settings](images/resource-type-auth-config.png)  
   The role settings are of course only respected if authentication is enabled.

## Admin-Events (kc-13-b3, kc-14-b2+)

If admin events are enabled the SCIM endpoint implementations will fire admin events of the following types:

* User [CREATE, UPDATE, DELETE]
* Group [CREATE, UPDATE, DELETE]
   * User GroupMembership [CREATE, DELETE]
   * Group GroupMembership [CREATE, DELETE]
* RealmRole [CREATE, UPDATE, DELETE]

Any event that triggers a `GroupMembership` change will also cause either a `Group_Create` event or a `Group_Update`
event depending on the fact if the resource is getting created or updated.

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

**NOTE**: Do not get confused with the roles here. These roles are only for accessing the management in the keycloak web
admin console. These roles have nothing to do with accessing the SCIM-endpoints.

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
