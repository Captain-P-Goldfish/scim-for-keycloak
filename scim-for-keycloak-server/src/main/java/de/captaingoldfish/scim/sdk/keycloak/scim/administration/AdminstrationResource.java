package de.captaingoldfish.scim.sdk.keycloak.scim.administration;

import javax.ws.rs.Path;

import org.keycloak.models.KeycloakSession;

import de.captaingoldfish.scim.sdk.keycloak.auth.Authentication;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractEndpoint;
import lombok.extern.slf4j.Slf4j;


/**
 * this endpoint is used to change the SCIM configuration. It requires the role
 * {@link de.captaingoldfish.scim.sdk.keycloak.provider.RealmRoleInitializer#SCIM_ADMIN_ROLE} to get access to
 * the endpoints
 * 
 * @author Pascal Knueppel
 * @since 27.07.2020
 */
@Slf4j
public class AdminstrationResource extends AbstractEndpoint
{

  public AdminstrationResource(KeycloakSession keycloakSession, Authentication authentication)
  {
    super(keycloakSession);
    authentication.authenticateAsScimAdmin(keycloakSession);
  }

  /**
   * handles the service provider configuration
   */
  @Path("/serviceProviderConfig")
  public ServiceProviderResource getServiceProviderResource()
  {
    return new ServiceProviderResource(getKeycloakSession());
  }

  /**
   * handles the resource type configuration
   */
  @Path("/resourceType")
  public ResourceTypeResource getResourceTypeResource()
  {
    return new ResourceTypeResource(getKeycloakSession());
  }
}
