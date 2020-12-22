package de.captaingoldfish.scim.sdk.keycloak.scim.administration;

import java.time.temporal.ChronoUnit;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ChangePasswordConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.ETagConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.FilterConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.SortConfig;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimServiceProviderServiceBridge;
import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;


/**
 * @author Pascal Knueppel
 * @since 08.08.2020
 */
public class ServiceProviderResourceTest extends KeycloakScimManagementTest
{

  /**
   * the endpoint under test
   */
  private ServiceProviderResource serviceProviderResource;


  /**
   * initializes the endpoint
   */
  @BeforeEach
  public void initTests()
  {
    serviceProviderResource = new ServiceProviderResource(getKeycloakSession());
  }

  /**
   * verifies that the configuration can successfully be updated
   */
  @Test
  public void testUpdateServiceProviderConfiguration()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .filterConfig(FilterConfig.builder()
                                                                               .supported(false)
                                                                               .maxResults(5)
                                                                               .build())
                                                     .sortConfig(SortConfig.builder().supported(false).build())
                                                     .patchConfig(PatchConfig.builder().supported(false).build())
                                                     .eTagConfig(ETagConfig.builder().supported(false).build())
                                                     .changePasswordConfig(ChangePasswordConfig.builder()
                                                                                               .supported(true)
                                                                                               .build())
                                                     .bulkConfig(BulkConfig.builder()
                                                                           .supported(false)
                                                                           .maxOperations(1)
                                                                           .maxPayloadSize(1L)
                                                                           .build())
                                                     .build();

    Response response = serviceProviderResource.updateServiceProviderConfig(serviceProvider.toString());
    Assertions.assertEquals(HttpStatus.OK, response.getStatus());

    verifyDatabaseSetupIsNotEqual(ScimServiceProviderServiceBridge.getDefaultServiceProvider(getKeycloakSession()));
    ServiceProvider updatedProvider = JsonHelper.readJsonDocument((String)response.getEntity(), ServiceProvider.class);
    verifyServiceProviderMatchesDatabaseEntry(updatedProvider);
    verifyServiceProviderMatchesDatabaseEntry(serviceProviderResource.getResourceEndpoint().getServiceProvider());
  }

  /**
   * verifies that the initial database data of the service provider matches the expected initial setup
   */
  private void verifyDatabaseSetupIsNotEqual(ServiceProvider sp)
  {
    ScimServiceProviderEntity serviceProvider = getEntityManager().createNamedQuery("getScimServiceProvider",
                                                                                    ScimServiceProviderEntity.class)
                                                                  .setParameter("realmId", getRealmModel().getId())
                                                                  .getSingleResult();
    Assertions.assertEquals(getRealmModel().getId(), serviceProvider.getRealmId());
    Assertions.assertNotEquals(sp.getFilterConfig().isSupported(), serviceProvider.isFilterSupported());
    Assertions.assertNotEquals(sp.getFilterConfig().getMaxResults(), serviceProvider.getFilterMaxResults());
    Assertions.assertNotEquals(sp.getSortConfig().isSupported(), serviceProvider.isSortSupported());
    Assertions.assertNotEquals(sp.getPatchConfig().isSupported(), serviceProvider.isPatchSupported());
    Assertions.assertNotEquals(sp.getETagConfig().isSupported(), serviceProvider.isEtagSupported());
    Assertions.assertNotEquals(sp.getChangePasswordConfig().isSupported(), serviceProvider.isChangePasswordSupported());
    Assertions.assertNotEquals(sp.getBulkConfig().isSupported(), serviceProvider.isBulkSupported());
    Assertions.assertNotEquals(sp.getBulkConfig().getMaxOperations(), serviceProvider.getBulkMaxOperations());
    Assertions.assertNotEquals(sp.getBulkConfig().getMaxPayloadSize(), serviceProvider.getBulkMaxPayloadSize());
    Assertions.assertNotEquals(sp.getMeta()
                                 .flatMap(Meta::getCreated)
                                 .orElseThrow(IllegalStateException::new)
                                 .truncatedTo(ChronoUnit.MILLIS),
                               serviceProvider.getCreated().truncatedTo(ChronoUnit.MILLIS));
    Assertions.assertNotEquals(sp.getMeta()
                                 .flatMap(Meta::getLastModified)
                                 .orElseThrow(IllegalStateException::new)
                                 .truncatedTo(ChronoUnit.MILLIS),
                               serviceProvider.getLastModified().truncatedTo(ChronoUnit.MILLIS));
  }

  /**
   * verifies that the initial database data of the service provider matches the expected initial setup
   */
  private void verifyServiceProviderMatchesDatabaseEntry(ServiceProvider sp)
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
    Assertions.assertEquals(sp.getMeta()
                              .flatMap(Meta::getCreated)
                              .orElseThrow(IllegalStateException::new)
                              .truncatedTo(ChronoUnit.MILLIS),
                            serviceProvider.getCreated().truncatedTo(ChronoUnit.MILLIS));
    Assertions.assertEquals(sp.getMeta()
                              .flatMap(Meta::getLastModified)
                              .orElseThrow(IllegalStateException::new)
                              .truncatedTo(ChronoUnit.MILLIS),
                            serviceProvider.getLastModified().truncatedTo(ChronoUnit.MILLIS));
  }
}
