package de.captaingoldfish.scim.sdk.keycloak.scim;

import java.util.Map;

import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;


/**
 * @author Pascal Knueppel
 * @since 07.08.2020
 */
public class ScimConfigurationBridge
{

  /**
   * @return all currently registered and configured endpoints
   */
  public static Map<String, ResourceEndpoint> getScimResourceEndpoints()
  {
    return ScimConfiguration.getRESOURCE_ENDPOINT_MAP();
  }

  /**
   * clears the current configuration from the cache map
   */
  public static void clearScimContext()
  {
    ScimConfiguration.getRESOURCE_ENDPOINT_MAP().clear();
  }
}
