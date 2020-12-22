package de.captaingoldfish.scim.sdk.keycloak.scim.administration;

import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;


/**
 * @author Pascal Knueppel
 * @since 07.08.2020
 */
public class AdminstrationResourceTest extends KeycloakScimManagementTest
{

  /**
   * initializes the endpoint
   */
  @Test
  public void testAuthentication()
  {
    AdminstrationResource administrationResource = new AdminstrationResource(getKeycloakSession(), getAuthentication());
  }


}
