package de.captaingoldfish.scim.sdk.keycloak.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.RoleAdapter;
import org.keycloak.models.jpa.entities.RoleEntity;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimConfigurationBridge;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimEndpoint;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimEndpointBridge;
import de.captaingoldfish.scim.sdk.keycloak.scim.administration.ResourceTypeResource;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.ParseableResourceType;
import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ETagFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.EndpointControlFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeAuthorization;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 08.08.2020
 */
@Slf4j
public class ScimResourceTypeServiceTest extends KeycloakScimManagementTest
{

  public static Stream<Arguments> getAvailableRoleParams()
  {
    return Stream.of(Arguments.arguments(Arrays.asList("admin", "user", "keycloak"), Arrays.asList()),
                     Arguments.arguments(Arrays.asList("admin", "user", "keycloak"), Arrays.asList("admin")),
                     Arguments.arguments(Arrays.asList("admin", "user", "keycloak"), Arrays.asList("admin", "user")),
                     Arguments.arguments(Arrays.asList("admin", "user", "keycloak"),
                                         Arrays.asList("admin", "user", "keycloak")));
  }

  /**
   * verifies that the available roles can be successfully retrieved
   * 
   * @param rolesToCreate the roles that should be created to execute this test
   * @param rolesAlreadySet the roles that should not be in the available roles result
   */
  @ParameterizedTest
  @MethodSource("getAvailableRoleParams")
  public void testGetAvailableRoles(List<String> rolesToCreate, List<String> rolesAlreadySet)
  {
    for ( String roleName : rolesToCreate )
    {
      getKeycloakSession().roles().addRealmRole(getRealmModel(), roleName);
    }
    ScimResourceTypeService resourceTypeService = new ScimResourceTypeService(getKeycloakSession());

    List<String> expectedRolesAvailable = rolesToCreate.stream()
                                                       .filter(roleName -> !rolesAlreadySet.contains(roleName))
                                                       .collect(Collectors.toList());
    Assertions.assertEquals(rolesToCreate.size() - rolesAlreadySet.size(), expectedRolesAvailable.size());

    Set<String> availableRoles = resourceTypeService.getAvailableRolesFor(new HashSet<>(rolesAlreadySet));
    log.warn(availableRoles.toString());
    MatcherAssert.assertThat(availableRoles,
                             Matchers.containsInAnyOrder(expectedRolesAvailable.stream()
                                                                               .map(Matchers::equalTo)
                                                                               .collect(Collectors.toList())));
  }

  /**
   * verifies that calling the endpoint {@link ResourceTypeResource#updateResourceType(String, String)} does
   * update the data in the database AND in the current running configuration
   */
  @Test
  public void testUpdateResourceType()
  {
    final String resourceTypeName = ResourceTypeNames.USER;

    final String description = "a new useless description";
    final boolean requireAuthentication = false;
    final boolean enabled = false;
    final boolean autoFiltering = false;
    final boolean autoSorting = false;
    final boolean eTagEnabled = true;
    final boolean createDisabled = true;
    final boolean getDisabled = true;
    final boolean listDisabled = true;
    final boolean updateDisabled = true;
    final boolean deleteDisabled = true;

    Collection<ResourceEndpoint> resourceEndpointCollection = ScimConfigurationBridge.getScimResourceEndpoints()
                                                                                     .values();
    Assertions.assertEquals(1, resourceEndpointCollection.size());
    ResourceEndpoint resourceEndpoint = resourceEndpointCollection.iterator().next();
    ResourceType currentUserResourceType = resourceEndpoint.getResourceTypeByName(resourceTypeName).get();

    // create access roles
    final RoleModel common = getKeycloakSession().roles().addRealmRole(getRealmModel(), "common");
    final RoleModel create = getKeycloakSession().roles().addRealmRole(getRealmModel(), "create");
    final RoleModel get = getKeycloakSession().roles().addRealmRole(getRealmModel(), "get");
    final RoleModel update = getKeycloakSession().roles().addRealmRole(getRealmModel(), "update");
    final RoleModel delete = getKeycloakSession().roles().addRealmRole(getRealmModel(), "delete");

    ParseableResourceType newResourceType = JsonHelper.copyResourceToObject(currentUserResourceType,
                                                                            ParseableResourceType.class);
    // set values for update
    {
      newResourceType.setDescription(description);
      ResourceTypeFeatures features = newResourceType.getFeatures();
      features.setResourceTypeDisabled(enabled);
      features.setAutoFiltering(autoFiltering);
      features.setAutoSorting(autoSorting);
      features.setETagFeature(ETagFeature.builder().enabled(eTagEnabled).build());

      EndpointControlFeature endpointControl = features.getEndpointControlFeature();
      endpointControl.setCreateDisabled(createDisabled);
      endpointControl.setGetDisabled(getDisabled);
      endpointControl.setListDisabled(listDisabled);
      endpointControl.setUpdateDisabled(updateDisabled);
      endpointControl.setDeleteDisabled(deleteDisabled);

      ResourceTypeAuthorization authorization = features.getAuthorization();
      authorization.setAuthenticated(requireAuthentication);

      authorization.setRoles(common.getName());
      authorization.setRolesCreate(create.getName());
      authorization.setRolesGet(get.getName());
      authorization.setRolesUpdate(update.getName());
      authorization.setRolesDelete(delete.getName());
    }

    ResourceTypeResource resourceTypeResource = new ResourceTypeResource(getKeycloakSession());
    Response response = resourceTypeResource.updateResourceType(newResourceType.getName(), newResourceType.toString());
    Assertions.assertEquals(HttpStatus.OK, response.getStatus());

    // now verify values from database
    {
      ScimResourceTypeService resourceTypeService = new ScimResourceTypeService(getKeycloakSession());
      ScimResourceTypeEntity resourceTypeEntity = resourceTypeService.getResourceTypeEntityByName(newResourceType.getName())
                                                                     .get();
      Assertions.assertEquals(description, resourceTypeEntity.getDescription());
      Assertions.assertEquals(enabled, resourceTypeEntity.isEnabled());
      Assertions.assertEquals(autoFiltering, resourceTypeEntity.isAutoFiltering());
      Assertions.assertEquals(autoSorting, resourceTypeEntity.isAutoSorting());
      Assertions.assertEquals(eTagEnabled, resourceTypeEntity.isEtagEnabled());
      Assertions.assertEquals(createDisabled, resourceTypeEntity.isDisableCreate());
      Assertions.assertEquals(getDisabled, resourceTypeEntity.isDisableGet());
      Assertions.assertEquals(listDisabled, resourceTypeEntity.isDisableList());
      Assertions.assertEquals(updateDisabled, resourceTypeEntity.isDisableUpdate());
      Assertions.assertEquals(deleteDisabled, resourceTypeEntity.isDisableDelete());

      Assertions.assertEquals(1, resourceTypeEntity.getEndpointRoles().size());
      Assertions.assertEquals(common.getName(), resourceTypeEntity.getEndpointRoles().get(0).getName());

      Assertions.assertEquals(1, resourceTypeEntity.getCreateRoles().size());
      Assertions.assertEquals(create.getName(), resourceTypeEntity.getCreateRoles().get(0).getName());

      Assertions.assertEquals(1, resourceTypeEntity.getGetRoles().size());
      Assertions.assertEquals(get.getName(), resourceTypeEntity.getGetRoles().get(0).getName());

      Assertions.assertEquals(1, resourceTypeEntity.getUpdateRoles().size());
      Assertions.assertEquals(update.getName(), resourceTypeEntity.getUpdateRoles().get(0).getName());

      Assertions.assertEquals(1, resourceTypeEntity.getDeleteRoles().size());
      Assertions.assertEquals(delete.getName(), resourceTypeEntity.getDeleteRoles().get(0).getName());
    }

    // now verify values from current configuration
    {
      ResourceType resourceType = resourceEndpoint.getResourceTypeByName(resourceTypeName).get();

      Assertions.assertEquals(description, resourceType.getDescription().get());
      Assertions.assertEquals(enabled, !resourceType.isDisabled());

      ResourceTypeFeatures features = resourceType.getFeatures();
      Assertions.assertEquals(autoFiltering, features.isAutoSorting());
      Assertions.assertEquals(autoSorting, features.isAutoSorting());
      Assertions.assertEquals(eTagEnabled, features.getETagFeature().isEnabled());

      EndpointControlFeature endpointControl = features.getEndpointControlFeature();
      Assertions.assertEquals(createDisabled, endpointControl.isCreateDisabled());
      Assertions.assertEquals(getDisabled, endpointControl.isGetDisabled());
      Assertions.assertEquals(listDisabled, endpointControl.isListDisabled());
      Assertions.assertEquals(updateDisabled, endpointControl.isUpdateDisabled());
      Assertions.assertEquals(deleteDisabled, endpointControl.isDeleteDisabled());

      ResourceTypeAuthorization authorization = features.getAuthorization();
      Assertions.assertEquals(requireAuthentication, authorization.isAuthenticated());

      Assertions.assertEquals(1, authorization.getRoles().size());
      Assertions.assertEquals(common.getName(), authorization.getRoles().iterator().next());

      Assertions.assertEquals(1, authorization.getRolesCreate().size());
      Assertions.assertEquals(create.getName(), authorization.getRolesCreate().iterator().next());

      Assertions.assertEquals(1, authorization.getRolesGet().size());
      Assertions.assertEquals(get.getName(), authorization.getRolesGet().iterator().next());

      Assertions.assertEquals(1, authorization.getRolesUpdate().size());
      Assertions.assertEquals(update.getName(), authorization.getRolesUpdate().iterator().next());

      Assertions.assertEquals(1, authorization.getRolesDelete().size());
      Assertions.assertEquals(delete.getName(), authorization.getRolesDelete().iterator().next());
    }
  }

  /**
   * will test that associated roles will be successfully removed from resource types on database level and from
   * configuration level
   */
  @TestFactory
  public List<DynamicTest> testRemoveAssociatedRoles()
  {
    List<DynamicTest> dynamicTestList = new ArrayList<>();

    // create access roles
    final RoleModel common = getKeycloakSession().roles().addRealmRole(getRealmModel(), "common");
    final RoleModel create = getKeycloakSession().roles().addRealmRole(getRealmModel(), "create");
    final RoleModel get = getKeycloakSession().roles().addRealmRole(getRealmModel(), "get");
    final RoleModel update = getKeycloakSession().roles().addRealmRole(getRealmModel(), "update");
    final RoleModel delete = getKeycloakSession().roles().addRealmRole(getRealmModel(), "delete");

    ScimResourceTypeService resourceTypeService = new ScimResourceTypeService(getKeycloakSession());
    ScimResourceTypeEntity userResourceTypeDb = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.USER)
                                                                   .get();
    ScimResourceTypeEntity groupResourceTypeDb = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.GROUPS)
                                                                    .get();

    Function<RoleModel, ArrayList<RoleEntity>> toModifiableRoleList = roleModel -> {
      return new ArrayList<>(Arrays.asList(((RoleAdapter)roleModel).getEntity()));
    };
    userResourceTypeDb.setEndpointRoles(toModifiableRoleList.apply(common));
    userResourceTypeDb.setCreateRoles(toModifiableRoleList.apply(create));
    userResourceTypeDb.setGetRoles(toModifiableRoleList.apply(get));
    userResourceTypeDb.setUpdateRoles(toModifiableRoleList.apply(update));
    userResourceTypeDb.setDeleteRoles(toModifiableRoleList.apply(delete));

    groupResourceTypeDb.setCreateRoles(toModifiableRoleList.apply(create));
    groupResourceTypeDb.setUpdateRoles(toModifiableRoleList.apply(update));
    // the groups resource type will not get any more roles

    dynamicTestList.add(DynamicTest.dynamicTest("reinitialize ScimEndpoint", () -> {
      ScimConfigurationBridge.getScimResourceEndpoints().clear();
      Assertions.assertDoesNotThrow(() -> setScimEndpoint(new ScimEndpoint(getKeycloakSession(), getAuthentication())));
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("check roles present on database level", () -> {
      ScimResourceTypeEntity userResourceTypeEntity = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.USER)
                                                                         .get();
      Assertions.assertEquals(1, userResourceTypeEntity.getEndpointRoles().size());
      Assertions.assertEquals(common.getId(), userResourceTypeEntity.getEndpointRoles().iterator().next().getId());
      Assertions.assertEquals(1, userResourceTypeEntity.getCreateRoles().size());
      Assertions.assertEquals(create.getId(), userResourceTypeEntity.getCreateRoles().iterator().next().getId());
      Assertions.assertEquals(1, userResourceTypeEntity.getGetRoles().size());
      Assertions.assertEquals(get.getId(), userResourceTypeEntity.getGetRoles().iterator().next().getId());
      Assertions.assertEquals(1, userResourceTypeEntity.getUpdateRoles().size());
      Assertions.assertEquals(update.getId(), userResourceTypeEntity.getUpdateRoles().iterator().next().getId());
      Assertions.assertEquals(1, userResourceTypeEntity.getDeleteRoles().size());
      Assertions.assertEquals(delete.getId(), userResourceTypeEntity.getDeleteRoles().iterator().next().getId());

      ScimResourceTypeEntity groupResourceTypeEntity = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.GROUPS)
                                                                          .get();
      Assertions.assertEquals(0, groupResourceTypeEntity.getEndpointRoles().size());
      Assertions.assertEquals(1, groupResourceTypeEntity.getCreateRoles().size());
      Assertions.assertEquals(create.getId(), groupResourceTypeEntity.getCreateRoles().iterator().next().getId());
      Assertions.assertEquals(0, groupResourceTypeEntity.getGetRoles().size());
      Assertions.assertEquals(1, groupResourceTypeEntity.getUpdateRoles().size());
      Assertions.assertEquals(update.getId(), groupResourceTypeEntity.getUpdateRoles().iterator().next().getId());
      Assertions.assertEquals(0, groupResourceTypeEntity.getDeleteRoles().size());
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("check roles present on current config level", () -> {
      ResourceEndpoint resourceEndpoint = ScimEndpointBridge.getResourceEndpoint(getScimEndpoint());

      ResourceType userResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.USER).get();
      ResourceTypeAuthorization userAuthorization = userResourceType.getFeatures().getAuthorization();
      Assertions.assertEquals(1, userAuthorization.getRoles().size());
      Assertions.assertEquals(common.getName(), userAuthorization.getRoles().iterator().next());
      Assertions.assertEquals(1, userAuthorization.getRolesCreate().size());
      Assertions.assertEquals(create.getName(), userAuthorization.getRolesCreate().iterator().next());
      Assertions.assertEquals(1, userAuthorization.getRolesGet().size());
      Assertions.assertEquals(get.getName(), userAuthorization.getRolesGet().iterator().next());
      Assertions.assertEquals(1, userAuthorization.getRolesUpdate().size());
      Assertions.assertEquals(update.getName(), userAuthorization.getRolesUpdate().iterator().next());
      Assertions.assertEquals(1, userAuthorization.getRolesDelete().size());
      Assertions.assertEquals(delete.getName(), userAuthorization.getRolesDelete().iterator().next());

      ResourceType groupResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.GROUPS).get();
      ResourceTypeAuthorization groupAuthorization = groupResourceType.getFeatures().getAuthorization();
      Assertions.assertEquals(0, groupAuthorization.getRoles().size());
      Assertions.assertEquals(1, groupAuthorization.getRolesCreate().size());
      Assertions.assertEquals(create.getName(), groupAuthorization.getRolesCreate().iterator().next());
      Assertions.assertEquals(0, groupAuthorization.getRolesGet().size());
      Assertions.assertEquals(1, groupAuthorization.getRolesUpdate().size());
      Assertions.assertEquals(update.getName(), groupAuthorization.getRolesUpdate().iterator().next());
      Assertions.assertEquals(0, groupAuthorization.getRolesDelete().size());
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("remove common role", () -> {
      getKeycloakSession().roles().removeRole(common);
      resourceTypeService.removeAssociatedRoles(common);
      getEntityManager().clear(); // clears the entity manager cache
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("check common role removed from database", () -> {
      // verify that associations have been removed from database
      ScimResourceTypeEntity userResourceType = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.USER)
                                                                   .get();
      Assertions.assertEquals(0, userResourceType.getEndpointRoles().size());
      Assertions.assertEquals(1, userResourceType.getCreateRoles().size());
      Assertions.assertEquals(create.getId(), userResourceType.getCreateRoles().iterator().next().getId());
      Assertions.assertEquals(1, userResourceType.getGetRoles().size());
      Assertions.assertEquals(get.getId(), userResourceType.getGetRoles().iterator().next().getId());
      Assertions.assertEquals(1, userResourceType.getUpdateRoles().size());
      Assertions.assertEquals(update.getId(), userResourceType.getUpdateRoles().iterator().next().getId());
      Assertions.assertEquals(1, userResourceType.getDeleteRoles().size());
      Assertions.assertEquals(delete.getId(), userResourceType.getDeleteRoles().iterator().next().getId());

      ScimResourceTypeEntity groupResourceType = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.GROUPS)
                                                                    .get();
      Assertions.assertEquals(0, groupResourceType.getEndpointRoles().size());
      Assertions.assertEquals(1, groupResourceType.getCreateRoles().size());
      Assertions.assertEquals(create.getId(), groupResourceType.getCreateRoles().iterator().next().getId());
      Assertions.assertEquals(0, groupResourceType.getGetRoles().size());
      Assertions.assertEquals(1, groupResourceType.getUpdateRoles().size());
      Assertions.assertEquals(update.getId(), groupResourceType.getUpdateRoles().iterator().next().getId());
      Assertions.assertEquals(0, groupResourceType.getDeleteRoles().size());
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("check common role removed from current config", () -> {
      // verify that associations have been removed from current configuration
      ResourceEndpoint resourceEndpoint = ScimEndpointBridge.getResourceEndpoint(getScimEndpoint());

      ResourceType userResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.USER).get();
      ResourceTypeAuthorization userAuthorization = userResourceType.getFeatures().getAuthorization();
      Assertions.assertEquals(0, userAuthorization.getRoles().size());
      Assertions.assertEquals(1, userAuthorization.getRolesCreate().size());
      Assertions.assertEquals(create.getName(), userAuthorization.getRolesCreate().iterator().next());
      Assertions.assertEquals(1, userAuthorization.getRolesGet().size());
      Assertions.assertEquals(get.getName(), userAuthorization.getRolesGet().iterator().next());
      Assertions.assertEquals(1, userAuthorization.getRolesUpdate().size());
      Assertions.assertEquals(update.getName(), userAuthorization.getRolesUpdate().iterator().next());
      Assertions.assertEquals(1, userAuthorization.getRolesDelete().size());
      Assertions.assertEquals(delete.getName(), userAuthorization.getRolesDelete().iterator().next());

      ResourceType groupResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.GROUPS).get();
      ResourceTypeAuthorization groupAuthorization = groupResourceType.getFeatures().getAuthorization();
      Assertions.assertEquals(0, groupAuthorization.getRoles().size());
      Assertions.assertEquals(1, groupAuthorization.getRolesCreate().size());
      Assertions.assertEquals(create.getName(), groupAuthorization.getRolesCreate().iterator().next());
      Assertions.assertEquals(0, groupAuthorization.getRolesGet().size());
      Assertions.assertEquals(1, groupAuthorization.getRolesUpdate().size());
      Assertions.assertEquals(update.getName(), groupAuthorization.getRolesUpdate().iterator().next());
      Assertions.assertEquals(0, groupAuthorization.getRolesDelete().size());
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("remove create role", () -> {
      getKeycloakSession().roles().removeRole(create);
      resourceTypeService.removeAssociatedRoles(create);
      getEntityManager().clear(); // clears the entity manager cache
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("check create role removed from database", () -> {
      // verify that associations have been removed from database
      ScimResourceTypeEntity userResourceType = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.USER)
                                                                   .get();
      Assertions.assertEquals(0, userResourceType.getEndpointRoles().size());
      Assertions.assertEquals(0, userResourceType.getCreateRoles().size());
      Assertions.assertEquals(1, userResourceType.getGetRoles().size());
      Assertions.assertEquals(get.getId(), userResourceType.getGetRoles().iterator().next().getId());
      Assertions.assertEquals(1, userResourceType.getUpdateRoles().size());
      Assertions.assertEquals(update.getId(), userResourceType.getUpdateRoles().iterator().next().getId());
      Assertions.assertEquals(1, userResourceType.getDeleteRoles().size());
      Assertions.assertEquals(delete.getId(), userResourceType.getDeleteRoles().iterator().next().getId());

      ScimResourceTypeEntity groupResourceType = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.GROUPS)
                                                                    .get();
      Assertions.assertEquals(0, groupResourceType.getEndpointRoles().size());
      Assertions.assertEquals(0, groupResourceType.getCreateRoles().size());
      Assertions.assertEquals(0, groupResourceType.getGetRoles().size());
      Assertions.assertEquals(1, groupResourceType.getUpdateRoles().size());
      Assertions.assertEquals(update.getId(), groupResourceType.getUpdateRoles().iterator().next().getId());
      Assertions.assertEquals(0, groupResourceType.getDeleteRoles().size());
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("check create role removed from current config", () -> {
      // verify that associations have been removed from current configuration
      ResourceEndpoint resourceEndpoint = ScimEndpointBridge.getResourceEndpoint(getScimEndpoint());

      ResourceType userResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.USER).get();
      ResourceTypeAuthorization userAuthorization = userResourceType.getFeatures().getAuthorization();
      Assertions.assertEquals(0, userAuthorization.getRoles().size());
      Assertions.assertEquals(0, userAuthorization.getRolesCreate().size());
      Assertions.assertEquals(1, userAuthorization.getRolesGet().size());
      Assertions.assertEquals(get.getName(), userAuthorization.getRolesGet().iterator().next());
      Assertions.assertEquals(1, userAuthorization.getRolesUpdate().size());
      Assertions.assertEquals(update.getName(), userAuthorization.getRolesUpdate().iterator().next());
      Assertions.assertEquals(1, userAuthorization.getRolesDelete().size());
      Assertions.assertEquals(delete.getName(), userAuthorization.getRolesDelete().iterator().next());

      ResourceType groupResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.GROUPS).get();
      ResourceTypeAuthorization groupAuthorization = groupResourceType.getFeatures().getAuthorization();
      Assertions.assertEquals(0, groupAuthorization.getRoles().size());
      Assertions.assertEquals(0, groupAuthorization.getRolesCreate().size());
      Assertions.assertEquals(0, groupAuthorization.getRolesGet().size());
      Assertions.assertEquals(1, groupAuthorization.getRolesUpdate().size());
      Assertions.assertEquals(update.getName(), groupAuthorization.getRolesUpdate().iterator().next());
      Assertions.assertEquals(0, groupAuthorization.getRolesDelete().size());
    }));


    dynamicTestList.add(DynamicTest.dynamicTest("remove get role", () -> {
      getKeycloakSession().roles().removeRole(get);
      resourceTypeService.removeAssociatedRoles(get);
      getEntityManager().clear(); // clears the entity manager cache
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("check get role removed from database", () -> {
      // verify that associations have been removed from database
      ScimResourceTypeEntity userResourceType = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.USER)
                                                                   .get();
      Assertions.assertEquals(0, userResourceType.getEndpointRoles().size());
      Assertions.assertEquals(0, userResourceType.getCreateRoles().size());
      Assertions.assertEquals(0, userResourceType.getGetRoles().size());
      Assertions.assertEquals(1, userResourceType.getUpdateRoles().size());
      Assertions.assertEquals(update.getId(), userResourceType.getUpdateRoles().iterator().next().getId());
      Assertions.assertEquals(1, userResourceType.getDeleteRoles().size());
      Assertions.assertEquals(delete.getId(), userResourceType.getDeleteRoles().iterator().next().getId());

      ScimResourceTypeEntity groupResourceType = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.GROUPS)
                                                                    .get();
      Assertions.assertEquals(0, groupResourceType.getEndpointRoles().size());
      Assertions.assertEquals(0, groupResourceType.getCreateRoles().size());
      Assertions.assertEquals(0, groupResourceType.getGetRoles().size());
      Assertions.assertEquals(1, groupResourceType.getUpdateRoles().size());
      Assertions.assertEquals(update.getId(), groupResourceType.getUpdateRoles().iterator().next().getId());
      Assertions.assertEquals(0, groupResourceType.getDeleteRoles().size());
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("check get role removed from current config", () -> {
      // verify that associations have been removed from current configuration
      ResourceEndpoint resourceEndpoint = ScimEndpointBridge.getResourceEndpoint(getScimEndpoint());

      ResourceType userResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.USER).get();
      ResourceTypeAuthorization userAuthorization = userResourceType.getFeatures().getAuthorization();
      Assertions.assertEquals(0, userAuthorization.getRoles().size());
      Assertions.assertEquals(0, userAuthorization.getRolesCreate().size());
      Assertions.assertEquals(0, userAuthorization.getRolesGet().size());
      Assertions.assertEquals(1, userAuthorization.getRolesUpdate().size());
      Assertions.assertEquals(update.getName(), userAuthorization.getRolesUpdate().iterator().next());
      Assertions.assertEquals(1, userAuthorization.getRolesDelete().size());
      Assertions.assertEquals(delete.getName(), userAuthorization.getRolesDelete().iterator().next());

      ResourceType groupResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.GROUPS).get();
      ResourceTypeAuthorization groupAuthorization = groupResourceType.getFeatures().getAuthorization();
      Assertions.assertEquals(0, groupAuthorization.getRoles().size());
      Assertions.assertEquals(0, groupAuthorization.getRolesCreate().size());
      Assertions.assertEquals(0, groupAuthorization.getRolesGet().size());
      Assertions.assertEquals(1, groupAuthorization.getRolesUpdate().size());
      Assertions.assertEquals(update.getName(), groupAuthorization.getRolesUpdate().iterator().next());
      Assertions.assertEquals(0, groupAuthorization.getRolesDelete().size());
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("remove update role", () -> {
      getKeycloakSession().roles().removeRole(update);
      resourceTypeService.removeAssociatedRoles(update);
      getEntityManager().clear(); // clears the entity manager cache
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("check update role removed from database", () -> {
      // verify that associations have been removed from database

      ScimResourceTypeEntity userResourceType = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.USER)
                                                                   .get();
      Assertions.assertEquals(0, userResourceType.getEndpointRoles().size());
      Assertions.assertEquals(0, userResourceType.getCreateRoles().size());
      Assertions.assertEquals(0, userResourceType.getGetRoles().size());
      Assertions.assertEquals(0, userResourceType.getUpdateRoles().size());
      Assertions.assertEquals(1, userResourceType.getDeleteRoles().size());
      Assertions.assertEquals(delete.getId(), userResourceType.getDeleteRoles().iterator().next().getId());

      ScimResourceTypeEntity groupResourceType = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.GROUPS)
                                                                    .get();
      Assertions.assertEquals(0, groupResourceType.getEndpointRoles().size());
      Assertions.assertEquals(0, groupResourceType.getCreateRoles().size());
      Assertions.assertEquals(0, groupResourceType.getGetRoles().size());
      Assertions.assertEquals(0, groupResourceType.getUpdateRoles().size());
      Assertions.assertEquals(0, groupResourceType.getDeleteRoles().size());
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("check update role removed from current config", () -> {
      // verify that associations have been removed from current configuration
      ResourceEndpoint resourceEndpoint = ScimEndpointBridge.getResourceEndpoint(getScimEndpoint());

      ResourceType userResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.USER).get();
      ResourceTypeAuthorization userAuthorization = userResourceType.getFeatures().getAuthorization();
      Assertions.assertEquals(0, userAuthorization.getRoles().size());
      Assertions.assertEquals(0, userAuthorization.getRolesCreate().size());
      Assertions.assertEquals(0, userAuthorization.getRolesGet().size());
      Assertions.assertEquals(0, userAuthorization.getRolesUpdate().size());
      Assertions.assertEquals(1, userAuthorization.getRolesDelete().size());
      Assertions.assertEquals(delete.getName(), userAuthorization.getRolesDelete().iterator().next());

      ResourceType groupResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.GROUPS).get();
      ResourceTypeAuthorization groupAuthorization = groupResourceType.getFeatures().getAuthorization();
      Assertions.assertEquals(0, groupAuthorization.getRoles().size());
      Assertions.assertEquals(0, groupAuthorization.getRolesCreate().size());
      Assertions.assertEquals(0, groupAuthorization.getRolesGet().size());
      Assertions.assertEquals(0, groupAuthorization.getRolesUpdate().size());
      Assertions.assertEquals(0, groupAuthorization.getRolesDelete().size());
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("remove delete role", () -> {
      getKeycloakSession().roles().removeRole(delete);
      resourceTypeService.removeAssociatedRoles(delete);
      getEntityManager().clear(); // clears the entity manager cache
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("check delete role removed from database", () -> {
      // verify that associations have been removed from database
      ScimResourceTypeEntity userResourceType = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.USER)
                                                                   .get();
      Assertions.assertEquals(0, userResourceType.getEndpointRoles().size());
      Assertions.assertEquals(0, userResourceType.getCreateRoles().size());
      Assertions.assertEquals(0, userResourceType.getGetRoles().size());
      Assertions.assertEquals(0, userResourceType.getUpdateRoles().size());
      Assertions.assertEquals(0, userResourceType.getDeleteRoles().size());

      ScimResourceTypeEntity groupResourceType = resourceTypeService.getResourceTypeEntityByName(ResourceTypeNames.GROUPS)
                                                                    .get();
      Assertions.assertEquals(0, groupResourceType.getEndpointRoles().size());
      Assertions.assertEquals(0, groupResourceType.getCreateRoles().size());
      Assertions.assertEquals(0, groupResourceType.getGetRoles().size());
      Assertions.assertEquals(0, groupResourceType.getUpdateRoles().size());
      Assertions.assertEquals(0, groupResourceType.getDeleteRoles().size());
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("check delete role removed from current config", () -> {
      // verify that associations have been removed from current configuration
      ResourceEndpoint resourceEndpoint = ScimEndpointBridge.getResourceEndpoint(getScimEndpoint());

      ResourceType userResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.USER).get();
      ResourceTypeAuthorization userAuthorization = userResourceType.getFeatures().getAuthorization();
      Assertions.assertEquals(0, userAuthorization.getRoles().size());
      Assertions.assertEquals(0, userAuthorization.getRolesCreate().size());
      Assertions.assertEquals(0, userAuthorization.getRolesGet().size());
      Assertions.assertEquals(0, userAuthorization.getRolesUpdate().size());
      Assertions.assertEquals(0, userAuthorization.getRolesDelete().size());

      ResourceType groupResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.GROUPS).get();
      ResourceTypeAuthorization groupAuthorization = groupResourceType.getFeatures().getAuthorization();
      Assertions.assertEquals(0, groupAuthorization.getRoles().size());
      Assertions.assertEquals(0, groupAuthorization.getRolesCreate().size());
      Assertions.assertEquals(0, groupAuthorization.getRolesGet().size());
      Assertions.assertEquals(0, groupAuthorization.getRolesUpdate().size());
      Assertions.assertEquals(0, groupAuthorization.getRolesDelete().size());
    }));


    return dynamicTestList;
  }
}
