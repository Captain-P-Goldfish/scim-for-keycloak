package de.captaingoldfish.scim.sdk.keycloak.provider;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

import de.captaingoldfish.scim.sdk.keycloak.constants.ContextPaths;


/**
 * author Pascal Knueppel <br>
 * created at: 04.02.2020 <br>
 * <br>
 * this class will setup the scim resource provider as a rest provider in the keycloak server
 *
 * @see META-INF/services/org.keycloak.services.resource.RealmResourceProviderFactory
 */
public class ScimEndpointProviderFactory implements RealmResourceProviderFactory
{

  /**
   * this ID identifies the rest provider and is used as base context path for this module
   */
  public static final String ID = ContextPaths.SCIM_BASE_PATH;

  /**
   * @return the ID of this module that is used as base context path
   */
  @Override
  public String getId()
  {
    return ID;
  }

  /**
   * keycloak calls this method to create an instance of {@link ScimEndpointProvider} with each incoming request
   *
   * @param session the {@link KeycloakSession} object holds the authentication details about the current user
   *          and the realm the user is currently using
   * @return the {@link ScimEndpointProvider}
   */
  @Override
  public RealmResourceProvider create(KeycloakSession session)
  {
    return new ScimEndpointProvider(session);
  }

  @Override
  public void init(Config.Scope config)
  {
    // do nothing
  }

  /**
   * initialize the keycloak-scim environment
   */
  @Override
  public void postInit(KeycloakSessionFactory factory)
  {
    RealmRoleInitializer.initializeRoles(factory);
  }

  @Override
  public void close()
  {
    // do nothing
  }

}
