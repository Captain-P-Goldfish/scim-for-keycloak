package de.captaingoldfish.scim.sdk.keycloak.services;

import org.keycloak.models.KeycloakSession;

import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;


/**
 * @author Pascal Knueppel
 * @since 07.08.2020
 */
public class ScimServiceProviderServiceBridge
{

  /**
   * retrieves the default service provider configuration
   */
  public static ServiceProvider getDefaultServiceProvider(KeycloakSession keycloakSession)
  {
    ScimServiceProviderService scimServiceProviderService = new ScimServiceProviderService(keycloakSession);
    ScimServiceProviderEntity scimServiceProviderEntity = scimServiceProviderService.getDefaultServiceProviderConfiguration();
    return scimServiceProviderService.toScimRepresentation(scimServiceProviderEntity);
  }
}
