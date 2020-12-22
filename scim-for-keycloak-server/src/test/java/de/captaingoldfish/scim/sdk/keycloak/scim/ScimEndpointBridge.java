package de.captaingoldfish.scim.sdk.keycloak.scim;

import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;


/**
 * @author Pascal Knueppel
 * @since 12.08.2020
 */
public class ScimEndpointBridge
{

  /**
   * grants access to protected method for unit tests
   */
  public static ResourceEndpoint getResourceEndpoint(ScimEndpoint scimEndpoint)
  {
    return scimEndpoint.getResourceEndpoint();
  }
}
