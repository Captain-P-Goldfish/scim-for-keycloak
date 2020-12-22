package de.captaingoldfish.scim.sdk.keycloak.scim;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimResourceTypeService;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimServiceProviderServiceBridge;
import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.EndpointControlFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeAuthorization;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;


/**
 * @author Pascal Knueppel
 * @since 07.08.2020
 */
public class ScimConfigurationTest extends KeycloakScimManagementTest
{

  private ResourceEndpoint resourceEndpoint;

  @BeforeEach
  public void initTests()
  {
    Map<String, ResourceEndpoint> endpointMap = ScimConfigurationBridge.getScimResourceEndpoints();
    Assertions.assertEquals(1, endpointMap.size());
    resourceEndpoint = endpointMap.values().iterator().next();
  }

  /**
   * verifies that the resource type configurations are set as expected on initial setup
   */
  @Test
  public void testInitialResourceTypeConfigurations()
  {
    ResourceType userResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.USER).get();
    verifyInitialResourceTypeSetup(userResourceType);
    verifyInitialResourceTypeDatabaseSetup(userResourceType);
    ResourceType groupResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.GROUPS).get();
    verifyInitialResourceTypeSetup(groupResourceType);
    verifyInitialResourceTypeDatabaseSetup(groupResourceType);
  }

  /**
   * verifies that the database representation does match the active configuration
   */
  private void verifyInitialResourceTypeDatabaseSetup(ResourceType resourceType)
  {
    ScimResourceTypeService scimResourceTypeService = new ScimResourceTypeService(getKeycloakSession());
    ScimResourceTypeEntity entity = scimResourceTypeService.getResourceTypeEntityByName(resourceType.getName()).get();

    Assertions.assertEquals(resourceType.isDisabled(), !entity.isEnabled());

    ResourceTypeFeatures features = resourceType.getFeatures();
    Assertions.assertEquals(features.isAutoFiltering(), entity.isAutoFiltering());
    Assertions.assertEquals(features.isAutoSorting(), entity.isAutoSorting());

    EndpointControlFeature endpointControl = features.getEndpointControlFeature();
    Assertions.assertEquals(endpointControl.isCreateDisabled(), entity.isDisableCreate());
    Assertions.assertEquals(endpointControl.isGetDisabled(), entity.isDisableGet());
    Assertions.assertEquals(endpointControl.isListDisabled(), entity.isDisableList());
    Assertions.assertEquals(endpointControl.isUpdateDisabled(), entity.isDisableUpdate());
    Assertions.assertEquals(endpointControl.isDeleteDisabled(), entity.isDisableDelete());

    ResourceTypeAuthorization authorization = features.getAuthorization();
    Assertions.assertEquals(authorization.isAuthenticated(), entity.isRequireAuthentication());
  }

  /**
   * checks that the configuration of the given resource type conforms to the default configuration
   */
  private void verifyInitialResourceTypeSetup(ResourceType resourceType)
  {
    Assertions.assertFalse(resourceType.isDisabled());

    ResourceTypeFeatures features = resourceType.getFeatures();
    Assertions.assertTrue(features.isAutoFiltering());
    Assertions.assertTrue(features.isAutoSorting());
    Assertions.assertFalse(features.isSingletonEndpoint());
    Assertions.assertFalse(features.getETagFeature().isEnabled());

    EndpointControlFeature endpointControl = features.getEndpointControlFeature();
    Assertions.assertFalse(endpointControl.isCreateDisabled());
    Assertions.assertFalse(endpointControl.isGetDisabled());
    Assertions.assertFalse(endpointControl.isListDisabled());
    Assertions.assertFalse(endpointControl.isUpdateDisabled());
    Assertions.assertFalse(endpointControl.isDeleteDisabled());

    ResourceTypeAuthorization authorization = features.getAuthorization();
    Assertions.assertTrue(authorization.isAuthenticated());
    Assertions.assertTrue(authorization.getRoles().isEmpty());
    Assertions.assertTrue(authorization.getRolesCreate().isEmpty());
    Assertions.assertTrue(authorization.getRolesGet().isEmpty());
    Assertions.assertTrue(authorization.getRolesUpdate().isEmpty());
    Assertions.assertTrue(authorization.getRolesDelete().isEmpty());
  }

  /**
   * verifies that the initial service provider in the database matches the initial service provider
   * configuration
   */
  @Test
  public void testInitialServiceProviderConfiguration()
  {
    ServiceProvider serviceProvider = verifyDefaultServiceProviderConfig();
    verifyDatabaseServiceProviderConfigurationOnInitialSetup(serviceProvider);
    // verifies the current active configuration. If this configuration does match too the configurations must be
    // equal
    verifyDatabaseServiceProviderConfigurationOnInitialSetup(resourceEndpoint.getServiceProvider());
  }

  /**
   * verifies that the initial database data of the service provider matches the expected initial setup
   */
  private void verifyDatabaseServiceProviderConfigurationOnInitialSetup(ServiceProvider sp)
  {
    ScimServiceProviderEntity serviceProvider = getEntityManager().createNamedQuery("getScimServiceProvider",
                                                                                    ScimServiceProviderEntity.class)
                                                                  .setParameter("realmId", getRealmModel().getId())
                                                                  .getSingleResult();
    Assertions.assertEquals(getRealmModel().getId(), serviceProvider.getRealmId());
    Assertions.assertEquals(sp.getFilterConfig().isSupported(), serviceProvider.isFilterSupported());
    Assertions.assertEquals(sp.getFilterConfig().getMaxResults(), serviceProvider.getFilterMaxResults());
    Assertions.assertEquals(sp.getSortConfig().isSupported(), serviceProvider.isSortSupported());
    Assertions.assertEquals(sp.getPatchConfig().isSupported(), serviceProvider.isPatchSupported());
    Assertions.assertEquals(sp.getETagConfig().isSupported(), serviceProvider.isEtagSupported());
    Assertions.assertEquals(sp.getChangePasswordConfig().isSupported(), serviceProvider.isChangePasswordSupported());
    Assertions.assertEquals(sp.getBulkConfig().isSupported(), serviceProvider.isBulkSupported());
    Assertions.assertEquals(sp.getBulkConfig().getMaxOperations(), serviceProvider.getBulkMaxOperations());
    Assertions.assertEquals(sp.getBulkConfig().getMaxPayloadSize(), serviceProvider.getBulkMaxPayloadSize());
    Assertions.assertNotNull(sp.getMeta().flatMap(Meta::getCreated).orElse(null));
    Assertions.assertNull(serviceProvider.getLastModified());
  }

  /**
   * verifies that the data of the initial setup matches the expected data
   */
  private ServiceProvider verifyDefaultServiceProviderConfig()
  {
    ServiceProvider serviceProvider = ScimServiceProviderServiceBridge.getDefaultServiceProvider(getKeycloakSession());
    Assertions.assertTrue(serviceProvider.getFilterConfig().isSupported());
    Assertions.assertEquals(50, serviceProvider.getFilterConfig().getMaxResults());
    Assertions.assertTrue(serviceProvider.getSortConfig().isSupported());
    Assertions.assertTrue(serviceProvider.getPatchConfig().isSupported());
    Assertions.assertTrue(serviceProvider.getETagConfig().isSupported());
    Assertions.assertFalse(serviceProvider.getChangePasswordConfig().isSupported());
    Assertions.assertTrue(serviceProvider.getBulkConfig().isSupported());
    Assertions.assertEquals(15, serviceProvider.getBulkConfig().getMaxOperations());
    Assertions.assertEquals(2 * 1024 * 1024, serviceProvider.getBulkConfig().getMaxPayloadSize());
    return serviceProvider;
  }
}
