/*
 * inspired by beercloak: https://github.com/dteleguin/beercloak
 */
module.factory('ServiceProvider', function ($resource)
{
  return $resource(authUrl + '/realms/:realm/scim/admin/serviceProviderConfig',
                   {},
                   {
                     update: {
                       method: 'PUT',
                       url: authUrl + '/realms/:realm/scim/admin/serviceProviderConfig'
                     },
                     availableAuthClients: {
                       method: 'GET',
                       url: authUrl + '/realms/:realm/scim/admin/serviceProviderConfig/availableClients',
                       isArray: true
                     }
                   });
});

module.factory('ServiceProviderLoader', function (Loader, ServiceProvider, $route, $q)
{
  return Loader.get(ServiceProvider, function ()
  {
    return {
      realm: $route.current.params.realm
    };
  });
});

/* ***************************************************************************************************** */

module.factory('ResourceType', function ($resource)
{
  return $resource(authUrl + '/realms/:realm/scim/v2/ResourceTypes/:name?sortBy=name&filter=name ne' +
                   ' "ServiceProviderConfig" and name ne "ResourceType" and name ne "Schema"',
                   {},
                   {
                     metaResourceTypes: {
                       method: 'GET',
                       url: authUrl + '/realms/:realm/scim/v2/ResourceTypes?sortBy=name&filter=name eq ' +
                            ' "ServiceProviderConfig" or name eq "ResourceType" or name eq "Schema"'
                     },
                     update: {
                       method: 'PUT',
                       url: authUrl + '/realms/:realm/scim/admin/resourceType/:name'
                     },
                     availableRoles: {
                       method: 'GET',
                       url: authUrl + '/realms/:realm/scim/admin/resourceType/availableRoles/:name'
                     }
                   });
});

module.factory('ResourceTypeLoader', function (Loader, ResourceType, $route, $q)
{
  return Loader.get(ResourceType, function ()
  {
    return {
      realm: $route.current.params.realm,
      name: $route.current.params.name
    };
  });
});
