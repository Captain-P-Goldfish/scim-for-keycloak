package de.captaingoldfish.scim.sdk.keycloak.scim;

import org.keycloak.models.KeycloakSession;

import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import lombok.Getter;


/**
 * a simple context object that is being passed to the resource handler implementations of the SCIM
 * {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler}
 * 
 * @author Pascal Knueppel
 * @since 21.06.2021
 */
public class ScimKeycloakContext extends Context
{

  /**
   * the keycloak session that is passed to the resource endpoints
   */
  @Getter
  private final KeycloakSession keycloakSession;

  public ScimKeycloakContext(KeycloakSession keycloakSession, Authorization authorization)
  {
    super(authorization);
    this.keycloakSession = keycloakSession;
  }
}
