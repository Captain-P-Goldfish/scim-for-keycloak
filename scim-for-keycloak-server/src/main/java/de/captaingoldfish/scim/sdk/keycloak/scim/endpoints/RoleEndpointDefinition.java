package de.captaingoldfish.scim.sdk.keycloak.scim.endpoints;

import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.custom.resources.RealmRole;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.RealmRoleHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;


/**
 * represents the roles endpoint definition that includes the resoruce-type definition the schema definition
 * and a {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler} implementation for
 * {@link RealmRole}s
 *
 * @author Pascal Knueppel
 * @since 16.08.2020
 */
public class RoleEndpointDefinition extends EndpointDefinition
{

  /**
   * location of the resource type json file
   */
  private static final String ROLE_RESOURCE_TYPE_LOCATION = "/de/captaingoldfish/scim/sdk/keycloak/custom"
                                                            + "/resourcetypes/role-resource-type.json";

  /**
   * location of the role-schema json file
   */
  private static final String ROLE_RESOURCE_SCHEMA_LOCATION = "/de/captaingoldfish/scim/sdk/keycloak/custom/resources"
                                                              + "/roles.json";

  public RoleEndpointDefinition(RealmRoleHandler realmRoleHandler)
  {
    super(JsonHelper.loadJsonDocument(ROLE_RESOURCE_TYPE_LOCATION),
          JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA_LOCATION), null, realmRoleHandler);
  }
}
