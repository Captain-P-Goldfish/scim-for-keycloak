package de.captaingoldfish.scim.sdk.keycloak.scim.endpoints;

import java.util.Arrays;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;


/**
 * @author Pascal Knueppel
 * @since 16.10.2021
 */
public class CustomUserEndpoint extends EndpointDefinition
{

  private static final String RESOURCE_TYPE_COUNTRY_USER_EXTENSION_LOCATION = "/resourcetypes/custom-user-resource"
                                                                              + "-type.json";

  private static final String COUNTRY_USER_SCHEMA_EXTENSION_LOCATION = "/schemas/country-user-extension.json";

  public CustomUserEndpoint(ResourceHandler resourceHandler)
  {
    super(JsonHelper.loadJsonDocument(RESOURCE_TYPE_COUNTRY_USER_EXTENSION_LOCATION),
          JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON),
          Arrays.asList(JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON),
                        JsonHelper.loadJsonDocument(COUNTRY_USER_SCHEMA_EXTENSION_LOCATION)),
          resourceHandler);
  }
}
