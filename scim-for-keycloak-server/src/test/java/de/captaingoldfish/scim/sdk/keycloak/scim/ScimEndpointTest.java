package de.captaingoldfish.scim.sdk.keycloak.scim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.RoleAdapter;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.BooleanNode;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.administration.ServiceProviderResource;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimResourceTypeService;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.EndpointControlFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeAuthorization;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 07.08.2020
 */
@Slf4j
public class ScimEndpointTest extends AbstractScimEndpointTest
{

  /**
   * verifies that a second instantiation of the endpoint does not create additional unwanted database entries
   */
  @Test
  public void testRecreateEndpoint()
  {
    // the endpoint is already initialized by method super.initializeEndpoint()
    new ScimEndpoint(getKeycloakSession(), getAuthentication());
    Assertions.assertEquals(1, countEntriesInTable(ScimServiceProviderEntity.class));
    Assertions.assertEquals(3, countEntriesInTable(ScimResourceTypeEntity.class));
  }

  /**
   * verify that the scim endpoint is accessible
   */
  @Test
  public void testScimEndpointTest()
  {
    ScimEndpoint scimEndpoint = getScimEndpoint();
    HttpServletRequest request = RequestBuilder.builder(scimEndpoint).endpoint(EndpointPaths.USERS).build();

    Response response = scimEndpoint.handleScimRequest(request);

    Assertions.assertEquals(HttpStatus.OK, response.getStatus());
  }

  /**
   * this test verifies that the scim endpoint cannot be accessed if scim was disabled for the specific realm
   */
  @Test
  public void testDisableScim()
  {
    ServiceProvider serviceProvider = getScimEndpoint().getResourceEndpoint().getServiceProvider();
    serviceProvider.set("enabled", BooleanNode.valueOf(false));
    ServiceProviderResource serviceProviderResource = getScimEndpoint().administerResources()
                                                                       .getServiceProviderResource();
    serviceProviderResource.updateServiceProviderConfig(serviceProvider.toString());

    ScimEndpoint scimEndpoint = getScimEndpoint();
    HttpServletRequest request = RequestBuilder.builder(scimEndpoint).endpoint(EndpointPaths.USERS).build();

    try
    {
      scimEndpoint.handleScimRequest(request);
      Assertions.fail("this point must not be reached");
    }
    catch (NotFoundException ex)
    {
      log.trace("everything's fine", ex);
    }
  }

  /**
   * creates a second realm and verifies that entries for both realms are present within the database
   */
  @Test
  public void testSetupScimForTwoRealms()
  {

    // try to load the users from the default realm. A single user should be returned
    {
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint()).endpoint(EndpointPaths.USERS).build();
      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.OK, response.getStatus());

      String responseString = (String)response.getEntity();
      ListResponse listResponse = JsonHelper.readJsonDocument(responseString, ListResponse.class);
      Assertions.assertEquals(1, listResponse.getTotalResults());
    }

    KeycloakContext context = getKeycloakSession().getContext();
    RealmModel newRealm = getKeycloakSession().realms().createRealm("2ndRealm");
    Mockito.doReturn(newRealm).when(context).getRealm();
    ScimConfiguration.getScimEndpoint(getKeycloakSession(), true);
    Assertions.assertEquals(2, ScimConfigurationBridge.getScimResourceEndpoints().size());
    enableScim();

    // now try to load the users from the other realm. An empty list should be returned
    {
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint()).endpoint(EndpointPaths.USERS).build();
      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.OK, response.getStatus());

      String responseString = (String)response.getEntity();
      ListResponse listResponse = JsonHelper.readJsonDocument(responseString, ListResponse.class);
      Assertions.assertEquals(0, listResponse.getTotalResults());
    }
  }

  /**
   * creates a second realm and verifies that scim is disabled for the new realm
   */
  @Test
  public void testScimDisabledForNewRealm()
  {

    // try to load the users from the default realm. A single user should be returned
    {
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint()).endpoint(EndpointPaths.USERS).build();
      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.OK, response.getStatus());

      String responseString = (String)response.getEntity();
      ListResponse listResponse = JsonHelper.readJsonDocument(responseString, ListResponse.class);
      Assertions.assertEquals(1, listResponse.getTotalResults());
    }

    KeycloakContext context = getKeycloakSession().getContext();
    RealmModel newRealm = getKeycloakSession().realms().createRealm("2ndRealm");
    Mockito.doReturn(newRealm).when(context).getRealm();
    ScimConfiguration.getScimEndpoint(getKeycloakSession(), true);
    Assertions.assertEquals(2, ScimConfigurationBridge.getScimResourceEndpoints().size());

    // now try to load the users from the other realm. It should fail because SCIM is not enabled
    try
    {
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint()).endpoint(EndpointPaths.USERS).build();
      getScimEndpoint().handleScimRequest(request);
      Assertions.fail("this point must not be reached");
    }
    catch (NotFoundException ex)
    {
      log.trace("everything's fine", ex);
    }
  }

  /**
   * creates a second realm, disables the default realm and shows that the new realm is still accessible while
   * the default realm is not
   */
  @Test
  public void testDisableDefaultRealmAndLeaveSecondRealmOpen()
  {
    KeycloakContext context = getKeycloakSession().getContext();
    RealmModel newRealm = getKeycloakSession().realms().createRealm("2ndRealm");
    // first thing: create the new realm and initialize the scim configuration
    {
      Mockito.doReturn(newRealm).when(context).getRealm();
      ScimConfiguration.getScimEndpoint(getKeycloakSession(), true);
      enableScim(); // for the second realm
      Assertions.assertEquals(2, ScimConfigurationBridge.getScimResourceEndpoints().size());
    }

    // switch back to default realm
    {
      Mockito.doReturn(getRealmModel()).when(context).getRealm();
    }

    // disable SCIM for default realm
    {
      ServiceProvider serviceProvider = getScimEndpoint().getResourceEndpoint().getServiceProvider();
      serviceProvider.set("enabled", BooleanNode.valueOf(false));
      ServiceProviderResource serviceProviderResource = getScimEndpoint().administerResources()
                                                                         .getServiceProviderResource();
      serviceProviderResource.updateServiceProviderConfig(serviceProvider.toString());
    }

    // try to load the users from the default realm. An exception should be thrown
    {
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint()).endpoint(EndpointPaths.USERS).build();
      try
      {
        getScimEndpoint().handleScimRequest(request);
        Assertions.fail("this point must not be reached");
      }
      catch (NotFoundException ex)
      {
        log.trace("everything's fine", ex);
      }
    }

    // switch back to the new realm
    {
      Mockito.doReturn(newRealm).when(context).getRealm();
    }

    // now try to load the users from the new realm. An empty list should be returned
    {
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint()).endpoint(EndpointPaths.USERS).build();
      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.OK, response.getStatus());

      String responseString = (String)response.getEntity();
      ListResponse listResponse = JsonHelper.readJsonDocument(responseString, ListResponse.class);
      Assertions.assertEquals(0, listResponse.getTotalResults());
    }
    // that proves that SCIM can be enabled and disabled for specific realms
  }

  /**
   * assures that on startup the configuration stored in the database is loaded instead of the default
   * configuration if the endpoint is setup
   */
  @ParameterizedTest
  @ValueSource(strings = {ResourceTypeNames.USER, ResourceTypeNames.GROUPS})
  public void testDatabaseConfigurationIsLoadedOnStartup(String resourceTypeName)
  {
    {
      // clear the current configuration which enables us to initiate a new pseudo startup
      ScimConfigurationBridge.getScimResourceEndpoints().clear();
    }
    ScimResourceTypeService resourceTypeService = new ScimResourceTypeService(getKeycloakSession());
    ScimResourceTypeEntity resourceTypeEntity = resourceTypeService.getResourceTypeEntityByName(resourceTypeName).get();

    // create access roles
    final RoleModel common = getKeycloakSession().roles().addRealmRole(getRealmModel(), "common");
    final RoleModel create = getKeycloakSession().roles().addRealmRole(getRealmModel(), "create");
    final RoleModel get = getKeycloakSession().roles().addRealmRole(getRealmModel(), "get");
    final RoleModel update = getKeycloakSession().roles().addRealmRole(getRealmModel(), "update");
    final RoleModel delete = getKeycloakSession().roles().addRealmRole(getRealmModel(), "delete");

    {
      // set the roles necessary to access the endpoints
      Function<RoleModel, ArrayList<RoleEntity>> toModifiableRoleList = roleModel -> {
        return new ArrayList<>(Arrays.asList(((RoleAdapter)roleModel).getEntity()));
      };
      resourceTypeEntity.setEndpointRoles(toModifiableRoleList.apply(common));
      resourceTypeEntity.setCreateRoles(toModifiableRoleList.apply(create));
      resourceTypeEntity.setGetRoles(toModifiableRoleList.apply(get));
      resourceTypeEntity.setUpdateRoles(toModifiableRoleList.apply(update));
      resourceTypeEntity.setDeleteRoles(toModifiableRoleList.apply(delete));
    }

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

    {
      // now set the rest of the values
      Assertions.assertNotEquals(requireAuthentication, resourceTypeEntity.isRequireAuthentication());
      resourceTypeEntity.setRequireAuthentication(false);

      Assertions.assertNotEquals(description, resourceTypeEntity.getDescription());
      resourceTypeEntity.setDescription(description);

      Assertions.assertNotEquals(enabled, resourceTypeEntity.isEnabled());
      resourceTypeEntity.setEnabled(enabled);

      Assertions.assertNotEquals(autoFiltering, resourceTypeEntity.isAutoFiltering());
      resourceTypeEntity.setAutoFiltering(autoFiltering);

      Assertions.assertNotEquals(autoSorting, resourceTypeEntity.isAutoSorting());
      resourceTypeEntity.setAutoSorting(autoSorting);

      Assertions.assertNotEquals(eTagEnabled, resourceTypeEntity.isEtagEnabled());
      resourceTypeEntity.setEtagEnabled(eTagEnabled);

      Assertions.assertNotEquals(createDisabled, resourceTypeEntity.isDisableCreate());
      resourceTypeEntity.setDisableCreate(createDisabled);

      Assertions.assertNotEquals(getDisabled, resourceTypeEntity.isDisableGet());
      resourceTypeEntity.setDisableGet(getDisabled);

      Assertions.assertNotEquals(listDisabled, resourceTypeEntity.isDisableList());
      resourceTypeEntity.setDisableList(listDisabled);

      Assertions.assertNotEquals(updateDisabled, resourceTypeEntity.isDisableUpdate());
      resourceTypeEntity.setDisableUpdate(updateDisabled);

      Assertions.assertNotEquals(deleteDisabled, resourceTypeEntity.isDisableDelete());
      resourceTypeEntity.setDisableDelete(deleteDisabled);
    }
    getEntityManager().merge(resourceTypeEntity);
    commitTransaction(); // commit
    beginTransaction(); // start a new transaction

    // now setup the configuration again just as if this were a new startup
    ScimEndpoint scimEndpoint = new ScimEndpoint(getKeycloakSession(), getAuthentication());
    setScimEndpoint(scimEndpoint);

    // now get the resource endpoint with the new configuration and verify that it matches the just saved
    // configuration in the database
    {
      ResourceType userResourceType = getScimEndpoint().getResourceEndpoint()
                                                       .getResourceTypeByName(resourceTypeName)
                                                       .get();

      Assertions.assertEquals(description, userResourceType.getDescription().get());
      ResourceTypeFeatures features = userResourceType.getFeatures();
      Assertions.assertEquals(enabled, !features.isResourceTypeDisabled());
      Assertions.assertEquals(autoFiltering, features.isAutoFiltering());
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
   * this test will show that the content-type "application/json" will be correctly replaced with
   * "application/scim+json"
   */
  @Test
  public void testCreateGroupWithApplicationJsonContentType()
  {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeader.CONTENT_TYPE_HEADER, "application/json");

    HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
    Mockito.doReturn(Collections.enumeration(headers.keySet())).when(servletRequest).getHeaderNames();
    Mockito.doReturn(headers.get(HttpHeader.CONTENT_TYPE_HEADER).toUpperCase(Locale.ROOT))
           .when(servletRequest)
           .getHeader(HttpHeader.CONTENT_TYPE_HEADER);

    Map<String, String> httpHeaders = getScimEndpoint().getHttpHeaders(servletRequest);
    Assertions.assertEquals(1, httpHeaders.size());
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE, httpHeaders.get(HttpHeader.CONTENT_TYPE_HEADER));
  }
}
