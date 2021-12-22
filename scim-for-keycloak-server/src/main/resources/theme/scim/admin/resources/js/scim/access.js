/*
 * inspired by beercloak: https://github.com/dteleguin/beercloak
 */
module.config(['$provide', function ($provide)
{
    
    $provide.decorator('$controller', ['$delegate', 'Auth', 'Current', function ($delegate, Auth, Current)
    {
        
        function getAccess(role)
        {
            
            if (!Current.realm)
            {
                return false;
            }
            
            var realmAccess = Auth.user && Auth.user['realm_access'];
            if (realmAccess)
            {
                realmAccess = realmAccess[Current.realm.realm];
                if (realmAccess)
                {
                    return realmAccess.indexOf(role) >= 0;
                }
            }
            return false;
        }
        
        return function (constructor, locals)
        {
            
            var controller = $delegate.apply(null, arguments);
            
            return angular.extend(function ()
                                  {
                
                                      var obj = controller();
                
                                      if (constructor === 'GlobalCtrl')
                                      {
                    
                                          Object.defineProperty(locals.$scope.access, 'viewScim', {
                                              get: function ()
                                              {
                                                  return getAccess("scim-admin");
                                              }
                                          });
                                      }
                
                                      return obj;
                
                                  }, controller);
            
        };
        
    }]);
    
}]);
