package de.captaingoldfish.scim.sdk.keycloak.scim.administration;

import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractEndpoint;
import de.captaingoldfish.scim.sdk.keycloak.services.UserLegacyDataMigrationService;


/**
 * @author Pascal Knueppel
 * @since 22.12.2022
 */
public class UserLegacyDataMigration extends AbstractEndpoint
{

  public UserLegacyDataMigration(KeycloakSession keycloakSession)
  {
    super(keycloakSession);
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateServiceProviderConfig()
  {
    UserLegacyDataMigrationService migrationService = new UserLegacyDataMigrationService(getKeycloakSession());

    migrationService.migrateUserData();

    ObjectNode response = new ObjectNode(JsonNodeFactory.instance);
    response.put("status", new TextNode("Migration was successfully executed"));
    return Response.ok().entity(response.toString()).build();
  }

}
