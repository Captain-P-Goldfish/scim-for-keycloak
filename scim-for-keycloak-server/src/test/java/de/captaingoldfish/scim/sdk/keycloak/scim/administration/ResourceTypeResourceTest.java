package de.captaingoldfish.scim.sdk.keycloak.scim.administration;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.ParseableResourceType;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimResourceTypeService;
import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ETagFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.EndpointControlFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeAuthorization;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;


/**
 * @author Pascal Knueppel
 * @since 08.08.2020
 */
public class ResourceTypeResourceTest extends KeycloakScimManagementTest
{

  /**
   * the endpoint under test
   */
  private ResourceTypeResource resourceTypeResource;


  /**
   * initializes the endpoint
   */
  @BeforeEach
  public void initTests()
  {
    resourceTypeResource = new ResourceTypeResource(getKeycloakSession());
  }

  /**
   * verifies that a resource type configuration can be successfully updated
   */
  @ParameterizedTest
  @ValueSource(strings = {ResourceTypeNames.USER, ResourceTypeNames.GROUPS})
  public void testUpdateResourceTypeConfig(String resourceTypeName)
  {
    final String description = "a useless description";
    final boolean autoFiltering = false;
    final boolean autoSorting = false;
    final boolean autoEtags = true;
    final boolean disableEndpoint = true;
    final boolean requireAuthentication = true;

    ParseableResourceType parseableResourceType = new ParseableResourceType();
    parseableResourceType.setName(resourceTypeName);
    parseableResourceType.setDescription(description);

    parseableResourceType.getFeatures().setAutoFiltering(autoFiltering);
    parseableResourceType.getFeatures().setAutoSorting(autoSorting);
    parseableResourceType.getFeatures().setETagFeature(ETagFeature.builder().enabled(autoEtags).build());
    parseableResourceType.getFeatures().setResourceTypeDisabled(disableEndpoint);

    parseableResourceType.getFeatures().getEndpointControlFeature().setCreateDisabled(disableEndpoint);
    parseableResourceType.getFeatures().getEndpointControlFeature().setGetDisabled(disableEndpoint);
    parseableResourceType.getFeatures().getEndpointControlFeature().setListDisabled(disableEndpoint);
    parseableResourceType.getFeatures().getEndpointControlFeature().setUpdateDisabled(disableEndpoint);
    parseableResourceType.getFeatures().getEndpointControlFeature().setDeleteDisabled(disableEndpoint);

    parseableResourceType.getFeatures().getAuthorization().setAuthenticated(requireAuthentication);

    Response response = resourceTypeResource.updateResourceType(resourceTypeName, parseableResourceType.toString());
    ParseableResourceType updatedResourceType = JsonHelper.readJsonDocument((String)response.getEntity(),
                                                                            ParseableResourceType.class);
    // removes the non updatable attributes from the response to prepare a comparison of the two given objects
    {
      updatedResourceType.remove(AttributeNames.RFC7643.SCHEMAS);
      updatedResourceType.remove(AttributeNames.RFC7643.ID);
      updatedResourceType.remove(AttributeNames.RFC7643.ENDPOINT);
      updatedResourceType.remove(AttributeNames.RFC7643.SCHEMA);
      updatedResourceType.remove(AttributeNames.RFC7643.SCHEMA_EXTENSIONS);
      updatedResourceType.remove(AttributeNames.RFC7643.META);
    }
    Assertions.assertEquals(parseableResourceType, updatedResourceType);
    compareDatabaseEntryWithReturnedData((String)response.getEntity());
    compareActualConfigWithReturnedData((String)response.getEntity());
  }

  /**
   * verifies that the given response content matches the data within the database
   */
  private void compareDatabaseEntryWithReturnedData(String responseContent)
  {
    ParseableResourceType updatedResourceType = JsonHelper.readJsonDocument(responseContent,
                                                                            ParseableResourceType.class);
    ScimResourceTypeService resourceTypeService = new ScimResourceTypeService(getKeycloakSession());

    final String resourceTypeName = updatedResourceType.getName();
    ScimResourceTypeEntity resourceTypeEntity = resourceTypeService.getResourceTypeEntityByName(resourceTypeName).get();
    Assertions.assertNotNull(resourceTypeEntity.getLastModified());
    Assertions.assertNotEquals(resourceTypeEntity.getCreated(), resourceTypeEntity.getLastModified());

    Assertions.assertEquals(updatedResourceType.getDescription().get(), resourceTypeEntity.getDescription());

    ResourceTypeFeatures features = updatedResourceType.getFeatures();
    Assertions.assertEquals(features.isResourceTypeDisabled(), !resourceTypeEntity.isEnabled());
    Assertions.assertEquals(features.isAutoFiltering(), resourceTypeEntity.isAutoFiltering());
    Assertions.assertEquals(features.isAutoSorting(), resourceTypeEntity.isAutoSorting());

    EndpointControlFeature endpointControl = features.getEndpointControlFeature();
    Assertions.assertEquals(endpointControl.isCreateDisabled(), resourceTypeEntity.isDisableCreate());
    Assertions.assertEquals(endpointControl.isGetDisabled(), resourceTypeEntity.isDisableGet());
    Assertions.assertEquals(endpointControl.isListDisabled(), resourceTypeEntity.isDisableList());
    Assertions.assertEquals(endpointControl.isUpdateDisabled(), resourceTypeEntity.isDisableUpdate());
    Assertions.assertEquals(endpointControl.isDeleteDisabled(), resourceTypeEntity.isDisableDelete());

    ResourceTypeAuthorization authorization = features.getAuthorization();
    Assertions.assertEquals(authorization.isAuthenticated(), resourceTypeEntity.isRequireAuthentication());
  }

  /**
   * verifies that the given response content matches the actual active configuration
   */
  private void compareActualConfigWithReturnedData(String responseContent)
  {
    ParseableResourceType updatedResourceType = JsonHelper.readJsonDocument(responseContent,
                                                                            ParseableResourceType.class);
    final String resourceTypeName = updatedResourceType.getName();
    ResourceType resourceType = resourceTypeResource.getResourceEndpoint()
                                                    .getResourceTypeByName(resourceTypeName)
                                                    .get();

    Assertions.assertEquals(updatedResourceType.getDescription().get(), resourceType.getDescription().get());

    ResourceTypeFeatures features = updatedResourceType.getFeatures();
    ResourceTypeFeatures actualFeatures = resourceType.getFeatures();
    Assertions.assertEquals(features.isResourceTypeDisabled(), actualFeatures.isResourceTypeDisabled());
    Assertions.assertEquals(features.isAutoFiltering(), actualFeatures.isAutoFiltering());
    Assertions.assertEquals(features.isAutoSorting(), actualFeatures.isAutoSorting());

    EndpointControlFeature endpointControl = features.getEndpointControlFeature();
    EndpointControlFeature actualEndpointControl = actualFeatures.getEndpointControlFeature();
    Assertions.assertEquals(endpointControl.isCreateDisabled(), actualEndpointControl.isCreateDisabled());
    Assertions.assertEquals(endpointControl.isGetDisabled(), actualEndpointControl.isGetDisabled());
    Assertions.assertEquals(endpointControl.isListDisabled(), actualEndpointControl.isListDisabled());
    Assertions.assertEquals(endpointControl.isUpdateDisabled(), actualEndpointControl.isUpdateDisabled());
    Assertions.assertEquals(endpointControl.isDeleteDisabled(), actualEndpointControl.isDeleteDisabled());

    ResourceTypeAuthorization authorization = features.getAuthorization();
    ResourceTypeAuthorization actualAuthorization = actualFeatures.getAuthorization();
    Assertions.assertEquals(authorization.isAuthenticated(), actualAuthorization.isAuthenticated());
  }
}
