module.directive('kcTabsScimList', function ()
{
  return {
    scope: true,
    restrict: 'E',
    replace: true,
    templateUrl: resourceUrl + '/templates/kc-tabs-scim-list.html'
  };
});
module.directive('kcTabsResourceType', function ()
{
  return {
    scope: true,
    restrict: 'E',
    replace: true,
    templateUrl: resourceUrl + '/templates/kc-tabs-resource-type.html'
  };
});
module.directive('kcTabsServiceProvider', function ()
{
  return {
    scope: true,
    restrict: 'E',
    replace: true,
    templateUrl: resourceUrl + '/templates/kc-tabs-service-provider.html'
  };
});
