package de.captaingoldfish.scim.sdk.keycloak.entities;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.models.jpa.RoleAdapter;

import de.captaingoldfish.scim.sdk.keycloak.services.ScimResourceTypeService;
import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 05.08.2020
 */
@Slf4j
public class ScimResourceTypeEntityTest extends KeycloakScimManagementTest
{

  /**
   * verifies that default of enabled is true
   */
  @Test
  public void testEnabled()
  {
    ScimResourceTypeEntity resourceType = ScimResourceTypeEntity.builder().build();
    Assertions.assertTrue(resourceType.isEnabled());

    resourceType = new ScimResourceTypeEntity();
    Assertions.assertTrue(resourceType.isEnabled());
  }

  /**
   * verifies that default of requiresAuthentication is true
   */
  @Test
  public void testRequireAuthentication()
  {
    ScimResourceTypeEntity resourceType = ScimResourceTypeEntity.builder().build();
    Assertions.assertTrue(resourceType.isRequireAuthentication());

    resourceType = new ScimResourceTypeEntity();
    Assertions.assertTrue(resourceType.isRequireAuthentication());
  }

  /**
   * verifies that a resource type can be created and persisted in the database
   */
  @Test
  public void testScimResourceTypeEntityTest()
  {
    deleteFromTable(ScimServiceProviderEntity.class);
    deleteFromTable(ScimResourceTypeEntity.class);
    beginTransaction();

    RoleAdapter scimTestRole = (RoleAdapter)getKeycloakSession().roles().addRealmRole(getRealmModel(), "scim-test");
    RoleAdapter createRole = (RoleAdapter)getKeycloakSession().roles().addRealmRole(getRealmModel(), "create");
    RoleAdapter getRole = (RoleAdapter)getKeycloakSession().roles().addRealmRole(getRealmModel(), "get");
    RoleAdapter updateRole = (RoleAdapter)getKeycloakSession().roles().addRealmRole(getRealmModel(), "update");
    RoleAdapter deleteRole = (RoleAdapter)getKeycloakSession().roles().addRealmRole(getRealmModel(), "delete");

    final String resourceTypeName = "User";
    ScimResourceTypeEntity resourceType = ScimResourceTypeEntity.builder()
                                                                .realmId(getRealmModel().getId())
                                                                .name(resourceTypeName)
                                                                .description("User Account")
                                                                .enabled(false)
                                                                .autoFiltering(true)
                                                                .autoSorting(true)
                                                                .disableCreate(true)
                                                                .disableGet(true)
                                                                .disableUpdate(true)
                                                                .disableDelete(true)
                                                                .requireAuthentication(false)
                                                                .endpointRoles(Arrays.asList(scimTestRole.getEntity()))
                                                                .createRoles(Arrays.asList(createRole.getEntity()))
                                                                .getRoles(Arrays.asList(getRole.getEntity()))
                                                                .updateRoles(Arrays.asList(updateRole.getEntity()))
                                                                .deleteRoles(Arrays.asList(deleteRole.getEntity()))
                                                                .build();
    persist(resourceType);
    commitTransaction();

    Assertions.assertEquals(1, countEntriesInMappingTable("SCIM_ENDPOINT_ROLES"));
    Assertions.assertEquals(1, countEntriesInMappingTable("SCIM_ENDPOINT_CREATE_ROLES"));
    Assertions.assertEquals(1, countEntriesInMappingTable("SCIM_ENDPOINT_GET_ROLES"));
    Assertions.assertEquals(1, countEntriesInMappingTable("SCIM_ENDPOINT_UPDATE_ROLES"));
    Assertions.assertEquals(1, countEntriesInMappingTable("SCIM_ENDPOINT_DELETE_ROLES"));
    Assertions.assertEquals(1, countEntriesInTable(ScimResourceTypeEntity.class));

    ScimResourceTypeService service = new ScimResourceTypeService(getKeycloakSession());
    ScimResourceTypeEntity scimResourceTypeEntity = service.getResourceTypeEntityByName(resourceTypeName).get();
    Assertions.assertEquals(scimTestRole.getEntity(), scimResourceTypeEntity.getEndpointRoles().get(0));
    Assertions.assertEquals(createRole.getEntity(), scimResourceTypeEntity.getCreateRoles().get(0));
    Assertions.assertEquals(getRole.getEntity(), scimResourceTypeEntity.getGetRoles().get(0));
    Assertions.assertEquals(updateRole.getEntity(), scimResourceTypeEntity.getUpdateRoles().get(0));
    Assertions.assertEquals(deleteRole.getEntity(), scimResourceTypeEntity.getDeleteRoles().get(0));
  }
}
