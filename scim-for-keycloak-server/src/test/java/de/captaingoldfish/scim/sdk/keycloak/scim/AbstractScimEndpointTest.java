package de.captaingoldfish.scim.sdk.keycloak.scim;

import com.fasterxml.jackson.databind.node.BooleanNode;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.keycloak.scim.administration.ServiceProviderResource;
import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;
import org.junit.jupiter.api.BeforeEach;


/**
 * Automatically enables the SCIM Endpoint for the default realm.
 *
 * @author Mario Siegenthaler
 * @since 07.08.2020
 */
public class AbstractScimEndpointTest extends KeycloakScimManagementTest
{

  @BeforeEach
  public void enableScim()
  {
    ServiceProvider serviceProvider = getScimEndpoint().getResourceEndpoint().getServiceProvider();
    serviceProvider.set("enabled", BooleanNode.valueOf(true));
    ServiceProviderResource serviceProviderResource = getScimEndpoint().administerResources()
                                                                       .getServiceProviderResource();
    serviceProviderResource.updateServiceProviderConfig(serviceProvider.toString());
  }
}
