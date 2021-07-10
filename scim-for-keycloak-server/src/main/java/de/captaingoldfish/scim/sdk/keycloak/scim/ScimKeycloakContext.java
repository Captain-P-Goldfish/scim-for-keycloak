package de.captaingoldfish.scim.sdk.keycloak.scim;

import org.keycloak.models.KeycloakSession;

import de.captaingoldfish.scim.sdk.keycloak.audit.ScimAdminEventBuilder;
import de.captaingoldfish.scim.sdk.keycloak.auth.ScimAuthorization;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
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

  /**
   * used to log admin events if on SCIM requests
   */
  @Getter
  private ScimAdminEventBuilder adminEventAuditer;

  public ScimKeycloakContext(KeycloakSession keycloakSession, ScimAuthorization authorization)
  {
    super(authorization);
    this.keycloakSession = keycloakSession;
    this.adminEventAuditer = createAnonymousEventBuilder();
    // if this consumer is called the adminEventAuditer will be overridden with an authenticated instance
    authorization.setAdminAuthConsumer(adminAuth -> {
      this.adminEventAuditer = new ScimAdminEventBuilder(keycloakSession, adminAuth);
    });
  }

  /**
   * creates an admin event builder for cases in which the authentication was deactivated on a specific resource
   * endpoints. In such cases the {@link ScimAuthorization#adminAuthConsumer} will not be executed and thus the
   * authentication object is missing for the admin event builder
   */
  private ScimAdminEventBuilder createAnonymousEventBuilder()
  {
    return new ScimAdminEventBuilder(keycloakSession, null);
  }
}
