/*
 * inspired by beercloak: https://github.com/dteleguin/beercloak
 */
module.controller('ServiceProviderController', function ($modal, $scope, realm, ServiceProvider, serviceProvider,
                                                         $location, $route, Dialog, Notifications)
{
    
    $scope.realm = realm;
    $scope.serviceProvider = serviceProvider;
    $scope.copy = angular.copy(serviceProvider);
    
    $scope.changed = false;
    $scope.disableSCIM = !$scope.serviceProvider.enabled;
    
    $scope.$watch('serviceProvider', function (serviceProvider)
    {
        if (!angular.equals($scope.copy, serviceProvider))
        {
            $scope.changed = true;
        }
        else
        {
            $scope.changed = false;
        }
    }, true);
    
    $scope.save = function ()
    {
        ServiceProvider.update(
          {
              realm: realm.realm
          },
          $scope.serviceProvider,
          function (response)
          {
              $scope.changed = false;
              $scope.serviceProvider = response;
              $scope.copy = angular.copy($scope.serviceProvider);
              $scope.disableSCIM = !$scope.serviceProvider.enabled;
              Notifications.success("Your changes have been saved to ServiceProvider.");
          }
        );
    };
    
    $scope.reset = function ()
    {
        $scope.serviceProvider = angular.copy($scope.copy);
        $scope.disableSCIM = !$scope.serviceProvider.enabled;
        $scope.changed = false;
    };
    
});

module.controller('ServiceProviderAuthController', function ($modal, $scope, realm, ServiceProvider, serviceProvider,
                                                             $location, $route, Dialog, Notifications)
{
    $scope.realm = realm;
    
    initDefaults = function (sp)
    {
        $scope.serviceProvider = sp;
        $scope.serviceProvider.authorizedClients = (typeof $scope.serviceProvider.authorizedClients === 'undefined')
                                                   ? []
                                                   : $scope.serviceProvider.authorizedClients;
        $scope.availableClients = ServiceProvider.availableAuthClients({realm: realm.realm});
        
        $scope.selectedAvailableClients = [];
        $scope.selectedAssignedClients = [];
    };
    
    initDefaults(serviceProvider);
    
    moveElementsFromTo = function (from, to, elements)
    {
        elements.forEach(function (element)
                         {
                             var index = from.indexOf(element);
                             if (index > -1)
                             {
                                 from.splice(index, 1);
                                 to.push(element);
                             }
                         });
    };
    
    $scope.addAuthorizedClients = function ()
    {
        moveElementsFromTo($scope.availableClients,
                           $scope.serviceProvider.authorizedClients,
                           $scope.selectedAvailableClients);
        ServiceProvider.update(
          {
              realm: realm.realm
          },
          $scope.serviceProvider,
          function (response)
          {
              initDefaults(response);
              Notifications.success("Your changes have been saved to ServiceProvider.");
          }
        );
    };
    
    $scope.removeAuthorizedClients = function ()
    {
        moveElementsFromTo($scope.serviceProvider.authorizedClients,
                           $scope.availableClients,
                           $scope.selectedAssignedClients);
        ServiceProvider.update(
          {
              realm: realm.realm
          },
          $scope.serviceProvider,
          function (response)
          {
              initDefaults(response);
              Notifications.success("Your changes have been saved to ServiceProvider.");
          }
        );
    };
});

module.controller('ResourceTypeListController', function ($scope, realm, ResourceType, resource)
{
    $scope.RESOURCE_TYPE_FEATURE_KEY = 'urn:gold:params:scim:schemas:extension:url:2.0:ResourceTypeFeatures';
    $scope.realm = realm;
    $scope.resource = resource;
    $scope.features = resource[$scope.RESOURCE_TYPE_FEATURE_KEY];
    $scope.metaResources = ResourceType.metaResourceTypes({realm: realm.realm});
    
    $scope.requiresAuthentication = function (resourceType)
    {
        var features = resourceType[$scope.RESOURCE_TYPE_FEATURE_KEY];
        return !features.hasOwnProperty('authorization') ||
               !features.authorization.hasOwnProperty('authenticated') ||
               features.authorization.authenticated;
    };
    
    $scope.isEnabled = function (resourceType)
    {
        var features = resourceType[$scope.RESOURCE_TYPE_FEATURE_KEY];
        features = (typeof features === 'undefined') ? {} : features;
        return !features.hasOwnProperty('disabled') || !features.disabled;
    };
});

module.controller('ResourceTypeController', function ($scope, Notifications, realm, ResourceType, resource)
{
    $scope.RESOURCE_TYPE_FEATURE_KEY = 'urn:gold:params:scim:schemas:extension:url:2.0:ResourceTypeFeatures';
    $scope.realm = realm;
    $scope.resource = resource;
    $scope.copy = angular.copy(resource);
    $scope.features = resource[$scope.RESOURCE_TYPE_FEATURE_KEY];
    $scope.features = $scope.features = (typeof $scope.features === 'undefined') ? {} : $scope.features;
    
    isEnabled = function ()
    {
        return !$scope.features.hasOwnProperty('disabled') || !$scope.features.disabled;
    };
    
    $scope.enabled = isEnabled();
    
    $scope.isEqual = function ()
    {
        return angular.equals($scope.copy, resource);
    };
    
    $scope.$watch('resource', function (resource)
    {
        if (!angular.equals($scope.copy, resource))
        {
            $scope.changed = true;
        }
        else
        {
            $scope.changed = false;
        }
    }, true);
    
    $scope.$watch('enabled', function (value)
    {
        $scope.features.disabled = !value;
    }, true);
    
    $scope.save = function ()
    {
        ResourceType.update(
          {
              realm: realm.realm,
              name: $scope.resource.name
          },
          $scope.resource,
          function (response)
          {
              $scope.changed = false;
              $scope.resource = response;
              $scope.features = $scope.resource[$scope.RESOURCE_TYPE_FEATURE_KEY];
              $scope.copy = angular.copy(response);
              Notifications.success("Your changes have been saved to resource type.");
          }
        );
    };
    
    $scope.reset = function ()
    {
        $scope.resource = angular.copy($scope.copy);
        $scope.features = $scope.resource[$scope.RESOURCE_TYPE_FEATURE_KEY];
        $scope.changed = false;
    };
    
});

module.controller('ResourceTypeAuthController', function ($scope, Notifications, realm, ResourceType, resource)
{
    $scope.RESOURCE_TYPE_FEATURE_KEY = 'urn:gold:params:scim:schemas:extension:url:2.0:ResourceTypeFeatures';
    $scope.AUTHORIZATION_FEATURE_KEY = 'authorization';
    $scope.realm = realm;
    
    getRequiresAuthentication = function ()
    {
        return !$scope.features.hasOwnProperty('authorization') ||
               !$scope.features.authorization.hasOwnProperty('authenticated') ||
               $scope.features.authorization.authenticated;
    };
    
    initDefaults = function (resource)
    {
        $scope.resource = resource;
        $scope.features = resource[$scope.RESOURCE_TYPE_FEATURE_KEY];
        $scope.featureAuth = $scope.features[$scope.AUTHORIZATION_FEATURE_KEY];
        
        $scope.featureAuth.roles = (typeof $scope.featureAuth.roles === 'undefined')
                                   ? []
                                   : $scope.featureAuth.roles;
        $scope.featureAuth.rolesCreate = (typeof $scope.featureAuth.rolesCreate === 'undefined')
                                         ? []
                                         : $scope.featureAuth.rolesCreate;
        $scope.featureAuth.rolesGet = (typeof $scope.featureAuth.rolesGet === 'undefined')
                                      ? []
                                      : $scope.featureAuth.rolesGet;
        $scope.featureAuth.rolesUpdate = (typeof $scope.featureAuth.rolesUpdate === 'undefined')
                                         ? []
                                         : $scope.featureAuth.rolesUpdate;
        $scope.featureAuth.rolesDelete = (typeof $scope.featureAuth.rolesDelete === 'undefined')
                                         ? []
                                         : $scope.featureAuth.rolesDelete;
        
        $scope.requiresAuthentication = getRequiresAuthentication();
        
        $scope.availableRoles = ResourceType.availableRoles({
                                                                realm: realm.realm,
                                                                name: resource.name
                                                            });
        $scope.selectedAvailableRoles = [];
        $scope.selectedAssignedRoles = [];
        $scope.selectedAvailableRolesCreate = [];
        $scope.selectedAssignedRolesCreate = [];
        $scope.selectedAvailableRolesGet = [];
        $scope.selectedAssignedRolesGet = [];
        $scope.selectedAvailableRolesUpdate = [];
        $scope.selectedAssignedRolesUpdate = [];
        $scope.selectedAvailableRolesDelete = [];
        $scope.selectedAssignedRolesDelete = [];
        
        $scope.copy = angular.copy(resource);
        $scope.changed = false;
    };
    
    initDefaults(resource);
    
    $scope.$watch('requiresAuthentication', function (requiresAuthentication)
    {
        if (!$scope.features.hasOwnProperty('authorization'))
        {
            $scope.features.authorization = {};
        }
        if (requiresAuthentication)
        {
            $scope.features.authorization.authenticated = true;
        }
        else
        {
            $scope.features.authorization.authenticated = false;
        }
    }, true);
    
    $scope.$watch('resource', function (resource)
    {
        if (!angular.equals($scope.copy, resource))
        {
            $scope.changed = true;
        }
        else
        {
            $scope.changed = false;
        }
    }, true);
    
    $scope.save = function ()
    {
        ResourceType.update(
          {
              realm: realm.realm,
              name: $scope.resource.name
          },
          $scope.resource,
          function (response)
          {
              $scope.resource = response;
              initDefaults(response);
              Notifications.success("Your changes have been saved to resource type.");
          }
        );
    };
    
    $scope.reset = function ()
    {
        $scope.resource = angular.copy($scope.copy);
        initDefaults($scope.copy);
        $scope.changed = false;
    };
    
    moveElementsFromTo = function (from, to, elements)
    {
        elements.forEach(function (element)
                         {
                             var index = from.indexOf(element);
                             if (index > -1)
                             {
                                 from.splice(index, 1);
                                 to.push(element);
                             }
                         });
    };
    
    $scope.updateRoleAccess = function (from, to, selected)
    {
        moveElementsFromTo(from, to, selected);
        selected.splice(0, selected.length);
        ResourceType.update(
          {
              realm: realm.realm,
              name: $scope.resource.name
          },
          $scope.resource,
          function (response)
          {
              $scope.resource = response;
              initDefaults(response);
              Notifications.success("Roles successfully added.");
          }
        );
    };
    
});
