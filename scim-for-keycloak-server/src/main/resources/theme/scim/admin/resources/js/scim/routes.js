/*
 * inspired by beercloak: https://github.com/dteleguin/beercloak
 */
module.config(['$routeProvider', function ($routeProvider)
{
    $routeProvider
      .when('/realms/:realm/scim/service-provider/settings', {
          templateUrl: resourceUrl + '/partials/service-provider.html',
          resolve: {
              realm: function (RealmLoader)
              {
                  return RealmLoader();
              },
              serviceProvider: function (ServiceProviderLoader)
              {
                  return ServiceProviderLoader();
              }
          },
          controller: 'ServiceProviderController'
      })
      .when('/realms/:realm/scim/service-provider/authorization', {
          templateUrl: resourceUrl + '/partials/service-provider-authorization.html',
          resolve: {
              realm: function (RealmLoader)
              {
                  return RealmLoader();
              },
              serviceProvider: function (ServiceProviderLoader)
              {
                  return ServiceProviderLoader();
              }
          },
          controller: 'ServiceProviderAuthController'
      })
      .when('/realms/:realm/scim/resource-type-list', {
          templateUrl: resourceUrl + '/partials/resource-type-list.html',
          resolve: {
              realm: function (RealmLoader)
              {
                  return RealmLoader();
              },
              resource: function (ResourceTypeLoader)
              {
                  return ResourceTypeLoader();
              }
          },
          controller: 'ResourceTypeListController'
      })
      .when('/realms/:realm/scim/resource-type/:name', {
          templateUrl: resourceUrl + '/partials/resource-type.html',
          resolve: {
              realm: function (RealmLoader)
              {
                  return RealmLoader();
              },
              resource: function (ResourceTypeLoader)
              {
                  return ResourceTypeLoader();
              }
          },
          controller: 'ResourceTypeController'
      })
      .when('/realms/:realm/scim/resource-type/roles/:name', {
          templateUrl: resourceUrl + '/partials/resource-type-role-mappings.html',
          resolve: {
              realm: function (RealmLoader)
              {
                  return RealmLoader();
              },
              resource: function (ResourceTypeLoader)
              {
                  return ResourceTypeLoader();
              }
          },
          controller: 'ResourceTypeAuthController'
      });
}]);
