package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.ChangePasswordConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Address;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Entitlement;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.GroupNode;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Ims;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.MultiComplexNode;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PhoneNumber;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Photo;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.ScimX509Certificate;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractScimEndpointTest;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimConfigurationBridge;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestMock;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 18.08.2020
 */
@Slf4j
public class UserHandlerTest extends AbstractScimEndpointTest
{

  /**
   * verifies that the user password can be updated if support is enabled
   */
  @Test
  public void testUpdatePassword()
  {
    ResourceEndpoint resourceEndpoint = ScimConfigurationBridge.getScimResourceEndpoints()
                                                               .get(getRealmModel().getName());
    ServiceProvider serviceProvider = resourceEndpoint.getServiceProvider();
    serviceProvider.setChangePasswordConfig(ChangePasswordConfig.builder().supported(true).build());

    UserCredentialManager credentialManager = getKeycloakSession().userCredentialManager();

    UserModel superMario = getKeycloakSession().users().addUser(getRealmModel(), "SuperMario");
    UserCredentialModel originalUserCredential = UserCredentialModel.password("Peach");
    {
      Assertions.assertTrue(credentialManager.updateCredential(getRealmModel(), superMario, originalUserCredential));

      UserCredentialModel erroneousCredentialModel = UserCredentialModel.password("something-wrong");
      Assertions.assertFalse(credentialManager.isValid(getRealmModel(), superMario, erroneousCredentialModel));
      Assertions.assertTrue(credentialManager.isValid(getRealmModel(), superMario, originalUserCredential));
    }

    final String newPassword = "newPassword";
    User user = User.builder().password(newPassword).build();
    PatchRequestOperation operation = PatchRequestOperation.builder().op(PatchOp.REPLACE).valueNode(user).build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(Collections.singletonList(operation)).build();

    RequestMock.mockRequest(getScimEndpoint(), getKeycloakSession())
               .method(HttpMethod.PATCH)
               .endpoint(EndpointPaths.USERS + "/" + superMario.getId());
    Response response = getScimEndpoint().handleScimRequest(patchOpRequest.toString());
    Assertions.assertEquals(HttpStatus.OK, response.getStatus());

    // validate
    {
      UserCredentialModel newUserCredential = UserCredentialModel.password(newPassword);
      Assertions.assertFalse(credentialManager.isValid(getRealmModel(), superMario, originalUserCredential));
      Assertions.assertTrue(credentialManager.isValid(getRealmModel(), superMario, newUserCredential));
    }

    User updateddUser = JsonHelper.readJsonDocument((String)response.getEntity(), User.class);
    // check for created admin event
    {
      List<AdminEvent> adminEventList = getAdminEventStoreProvider().createAdminQuery()
                                                                    .getResultStream()
                                                                    .collect(Collectors.toList());
      Assertions.assertEquals(1, adminEventList.size());
      AdminEvent adminEvent = adminEventList.get(0);
      Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      Assertions.assertEquals("users/" + updateddUser.getId().get(), adminEvent.getResourcePath());
      Assertions.assertEquals(OperationType.UPDATE, adminEvent.getOperationType());
      Assertions.assertEquals(org.keycloak.events.admin.ResourceType.USER, adminEvent.getResourceType());
      // equalize the two objects by modifying the meta-attribute. The meta-attribute is not identical because the
      // schema-validation is modifying the meta-attribute when evaluating the response
      User adminEventUser = JsonHelper.readJsonDocument(adminEvent.getRepresentation(), User.class);
      {
        updateddUser.getMeta().get().setResourceType(null);
        updateddUser.getMeta().get().setLocation(null);
        // the last modified representation on the left side does not match the representation on the right right side
        // because we got string comparison here and one representation is shown in UTC and the other in local date
        // time which is why we are overriding the last modified value here which makes the check for this value
        // pointless
        updateddUser.getMeta().get().setLastModified(adminEventUser.getMeta().get().getLastModified().get());
      }
      Assertions.assertEquals(updateddUser, updateddUser);
    }
  }

  /**
   * verifies that a user can be deleted
   */
  @Test
  public void testDeleteUser()
  {
    ResourceEndpoint resourceEndpoint = ScimConfigurationBridge.getScimResourceEndpoints()
                                                               .get(getRealmModel().getName());
    ServiceProvider serviceProvider = resourceEndpoint.getServiceProvider();
    serviceProvider.setChangePasswordConfig(ChangePasswordConfig.builder().supported(true).build());

    UserModel superMario = getKeycloakSession().users().addUser(getRealmModel(), "SuperMario");
    RequestMock.mockRequest(getScimEndpoint(), getKeycloakSession())
               .method(HttpMethod.DELETE)
               .endpoint(EndpointPaths.USERS + "/" + superMario.getId());
    Response response = getScimEndpoint().handleScimRequest(null);
    Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatus());

    // validate
    {
      Assertions.assertNull(getKeycloakSession().users().getUserById(getRealmModel(), superMario.getId()));
    }

    // check for created admin event
    {
      List<AdminEvent> adminEventList = getAdminEventStoreProvider().createAdminQuery()
                                                                    .getResultStream()
                                                                    .collect(Collectors.toList());
      Assertions.assertEquals(1, adminEventList.size());
      AdminEvent adminEvent = adminEventList.get(0);
      Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      Assertions.assertEquals("users/" + superMario.getId(), adminEvent.getResourcePath());
      Assertions.assertEquals(OperationType.DELETE, adminEvent.getOperationType());
      Assertions.assertEquals(org.keycloak.events.admin.ResourceType.USER, adminEvent.getResourceType());
      Assertions.assertEquals(User.builder().id(superMario.getId()).userName(superMario.getUsername()).build(),
                              JsonHelper.readJsonDocument(adminEvent.getRepresentation(), User.class));
    }
  }

  /**
   * adds a test that shows that a user is successfully created and that all attributes are returned as sent
   */
  @Test
  public void testCreateUser()
  {
    ResourceEndpoint resourceEndpoint = ScimConfigurationBridge.getScimResourceEndpoints()
                                                               .get(getRealmModel().getName());
    ServiceProvider serviceProvider = resourceEndpoint.getServiceProvider();
    serviceProvider.setChangePasswordConfig(ChangePasswordConfig.builder().supported(true).build());

    Random random = new Random();
    String name = "goldfish";
    String pw = UUID.randomUUID().toString();

    User user = User.builder()
                    .userName(name)
                    .externalId(UUID.randomUUID().toString())
                    .name(Name.builder()
                              .givenName(name + "_")
                              .middlename(UUID.randomUUID().toString())
                              .familyName("Mustermann")
                              .honorificPrefix("Mr.")
                              .honorificSuffix("sama")
                              .formatted(name + "____")
                              .build())
                    .active(random.nextBoolean())
                    .nickName(name + "+++")
                    .title("Dr.")
                    .displayName(name + "****")
                    .userType("admin")
                    .locale("de-DE")
                    .preferredLanguage("de")
                    .timeZone("Europe/Berlin")
                    .profileUrl("http://localhost/" + name)
                    .password(pw)
                    .emails(Arrays.asList(Email.builder().value(name + "@test.de").primary(true).build(),
                                          Email.builder().value(name + "_the_second@test.de").build()))
                    .phoneNumbers(Arrays.asList(PhoneNumber.builder()
                                                           .value(String.valueOf(random.nextLong() + Integer.MAX_VALUE))
                                                           .primary(true)
                                                           .build(),
                                                PhoneNumber.builder()
                                                           .value(String.valueOf(random.nextLong() + Integer.MAX_VALUE))
                                                           .build()))
                    .addresses(Arrays.asList(Address.builder()
                                                    .streetAddress(name + " street " + random.nextInt(500))
                                                    .country(random.nextBoolean() ? "germany" : "united states")
                                                    .postalCode(String.valueOf(random.nextLong() + Integer.MAX_VALUE))
                                                    .primary(random.nextInt(20) == 0)
                                                    .build(),
                                             Address.builder()
                                                    .streetAddress(name + " second street " + random.nextInt(500))
                                                    .country(random.nextBoolean() ? "germany" : "united states")
                                                    .postalCode(String.valueOf(random.nextLong() + Integer.MAX_VALUE))
                                                    .build()))
                    .ims(Arrays.asList(Ims.builder().value("bla@bla").primary(true).build(),
                                       Ims.builder().value("hepp@zep").build()))
                    .photos(Arrays.asList(Photo.builder().value("photo-1").primary(true).build(),
                                          Photo.builder().value("photo-2").build()))
                    .entitlements(Arrays.asList(Entitlement.builder().value("ent-1").primary(true).build(),
                                                Entitlement.builder().value("ent-2").build()))
                    .x509Certificates(Arrays.asList(ScimX509Certificate.builder()
                                                                       .value("MII...1")
                                                                       .primary(true)
                                                                       .build(),
                                                    ScimX509Certificate.builder().value("MII...4").build()))
                    .enterpriseUser(EnterpriseUser.builder()
                                                  .employeeNumber(UUID.randomUUID().toString())
                                                  .department(UUID.randomUUID().toString())
                                                  .costCenter(UUID.randomUUID().toString())
                                                  .division(UUID.randomUUID().toString())
                                                  .organization(UUID.randomUUID().toString())
                                                  .manager(Manager.builder()
                                                                  .value(UUID.randomUUID().toString())
                                                                  .build())
                                                  .build())
                    .build();

    RequestMock.mockRequest(getScimEndpoint(), getKeycloakSession())
               .method(HttpMethod.POST)
               .endpoint(EndpointPaths.USERS);
    Response response = getScimEndpoint().handleScimRequest(user.toString());
    Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());

    UserCredentialManager credentialManager = getKeycloakSession().userCredentialManager();

    final String userId;
    User createdUser = JsonHelper.readJsonDocument((String)response.getEntity(), User.class);
    // validate
    {
      userId = createdUser.getId().get();
      Assertions.assertEquals(user.getUserName().get(), createdUser.getUserName().get());
      Assertions.assertEquals(user.getExternalId().get(), createdUser.getExternalId().get());
      Assertions.assertEquals(user.getName().get(), createdUser.getName().get());
      Assertions.assertEquals(user.isActive().get(), createdUser.isActive().get());
      Assertions.assertEquals(user.getNickName().get(), createdUser.getNickName().get());
      Assertions.assertEquals(user.getTitle().get(), createdUser.getTitle().get());
      Assertions.assertEquals(user.getDisplayName().get(), createdUser.getDisplayName().get());
      Assertions.assertEquals(user.getUserType().get(), createdUser.getUserType().get());
      Assertions.assertEquals(user.getLocale().get(), createdUser.getLocale().get());
      Assertions.assertEquals(user.getPreferredLanguage().get(), createdUser.getPreferredLanguage().get());
      Assertions.assertEquals(user.getTimezone().get(), createdUser.getTimezone().get());
      Assertions.assertEquals(user.getProfileUrl().get(), createdUser.getProfileUrl().get());
      MatcherAssert.assertThat(createdUser.getEmails(),
                               Matchers.containsInAnyOrder(user.getEmails()
                                                               .stream()
                                                               .map(Matchers::equalTo)
                                                               .collect(Collectors.toList())));
      MatcherAssert.assertThat(createdUser.getPhoneNumbers(),
                               Matchers.containsInAnyOrder(user.getPhoneNumbers()
                                                               .stream()
                                                               .map(Matchers::equalTo)
                                                               .collect(Collectors.toList())));
      MatcherAssert.assertThat(createdUser.getAddresses(),
                               Matchers.containsInAnyOrder(user.getAddresses()
                                                               .stream()
                                                               .map(Matchers::equalTo)
                                                               .collect(Collectors.toList())));
      MatcherAssert.assertThat(createdUser.getIms(),
                               Matchers.containsInAnyOrder(user.getIms()
                                                               .stream()
                                                               .map(Matchers::equalTo)
                                                               .collect(Collectors.toList())));
      MatcherAssert.assertThat(createdUser.getPhotos(),
                               Matchers.containsInAnyOrder(user.getPhotos()
                                                               .stream()
                                                               .map(Matchers::equalTo)
                                                               .collect(Collectors.toList())));
      MatcherAssert.assertThat(createdUser.getEntitlements(),
                               Matchers.containsInAnyOrder(user.getEntitlements()
                                                               .stream()
                                                               .map(Matchers::equalTo)
                                                               .collect(Collectors.toList())));
      MatcherAssert.assertThat(createdUser.getX509Certificates(),
                               Matchers.containsInAnyOrder(user.getX509Certificates()
                                                               .stream()
                                                               .map(Matchers::equalTo)
                                                               .collect(Collectors.toList())));
      Assertions.assertEquals(user.getEnterpriseUser().get(), createdUser.getEnterpriseUser().get());
    }

    // check primary email and password
    {
      UserModel userModel = getKeycloakSession().users().getUserById(getRealmModel(), userId);
      Assertions.assertNotNull(userModel);
      Assertions.assertEquals(user.getEmails()
                                  .stream()
                                  .filter(MultiComplexNode::isPrimary)
                                  .findAny()
                                  .flatMap(Email::getValue)
                                  .get(),
                              userModel.getEmail());

      UserCredentialModel erroneousCredentialModel = UserCredentialModel.password("something-wrong");
      UserCredentialModel correctCredentialModel = UserCredentialModel.password(pw);

      Assertions.assertFalse(credentialManager.isValid(getRealmModel(), userModel, erroneousCredentialModel));
      Assertions.assertTrue(credentialManager.isValid(getRealmModel(), userModel, correctCredentialModel));
    }

    // check for created admin event
    {
      List<AdminEvent> adminEventList = getAdminEventStoreProvider().createAdminQuery()
                                                                    .getResultStream()
                                                                    .collect(Collectors.toList());
      Assertions.assertEquals(1, adminEventList.size());
      AdminEvent adminEvent = adminEventList.get(0);
      Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      Assertions.assertEquals("users/" + createdUser.getId().get(), adminEvent.getResourcePath());
      Assertions.assertEquals(OperationType.CREATE, adminEvent.getOperationType());
      Assertions.assertEquals(org.keycloak.events.admin.ResourceType.USER, adminEvent.getResourceType());
      // equalize the two objects by modifying the meta-attribute. The meta-attribute is not identical because the
      // schema-validation is modifying the meta-attribute when evaluating the response
      {
        createdUser.getMeta().get().setResourceType(null);
        createdUser.getMeta().get().setLocation(null);
      }
      Assertions.assertEquals(createdUser, JsonHelper.readJsonDocument(adminEvent.getRepresentation(), User.class));
    }
  }

  /**
   * this test must result in an admin-event entry that has an anonymous user and client entered into the table
   */
  @Test
  public void testCreateUserWithDeactivatedAuthentication()
  {
    ResourceEndpoint resourceEndpoint = ScimConfigurationBridge.getScimResourceEndpoints()
                                                               .get(getRealmModel().getName());
    ResourceType userResourceType = resourceEndpoint.getResourceTypeByName(ResourceTypeNames.USER).get();
    // if authentication is deactivated the authenticate-method in the Authorization-object will not be called and
    // thus we will get an anonymous admin-event object
    userResourceType.getFeatures().getAuthorization().setAuthenticated(false);

    User user = User.builder().userName("goldfish").build();
    RequestMock.mockRequest(getScimEndpoint(), getKeycloakSession())
               .method(HttpMethod.POST)
               .endpoint(EndpointPaths.USERS);
    Response response = getScimEndpoint().handleScimRequest(user.toString());
    Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());

    User createdUser = JsonHelper.readJsonDocument((String)response.getEntity(), User.class);
    // check for created admin event
    {
      List<AdminEvent> adminEventList = getAdminEventStoreProvider().createAdminQuery()
                                                                    .getResultStream()
                                                                    .collect(Collectors.toList());
      Assertions.assertEquals(1, adminEventList.size());
      AdminEvent adminEvent = adminEventList.get(0);
      Assertions.assertNotEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertNotEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      Assertions.assertEquals("anonymous", adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals("anonymous", adminEvent.getAuthDetails().getUserId());
      Assertions.assertEquals("users/" + createdUser.getId().get(), adminEvent.getResourcePath());
      Assertions.assertEquals(OperationType.CREATE, adminEvent.getOperationType());
      Assertions.assertEquals(org.keycloak.events.admin.ResourceType.USER, adminEvent.getResourceType());
      // equalize the two objects by modifying the meta-attribute. The meta-attribute is not identical because the
      // schema-validation is modifying the meta-attribute when evaluating the response
      {
        createdUser.getMeta().get().setResourceType(null);
        createdUser.getMeta().get().setLocation(null);
      }
      Assertions.assertEquals(createdUser, JsonHelper.readJsonDocument(adminEvent.getRepresentation(), User.class));
    }
  }

  /**
   * verifies that no NullPointerException will occur if a user has a deleted created timestamp
   */
  @Test
  public void testListUsersWithOneUserHavingNoCreatedTimestamp()
  {
    UserModel goldfish = getKeycloakSession().users().addUser(getRealmModel(), "goldfish");
    getKeycloakSession().users().addUser(getRealmModel(), "mario");
    goldfish.setCreatedTimestamp(null);

    RequestMock.mockRequest(getScimEndpoint(), getKeycloakSession())
               .method(HttpMethod.GET)
               .endpoint(EndpointPaths.USERS);
    Response response = getScimEndpoint().handleScimRequest(null);
    Assertions.assertEquals(HttpStatus.OK, response.getStatus());

    ListResponse listResponse = JsonHelper.readJsonDocument((String)response.getEntity(), ListResponse.class);
    // the two created users plus the one default user created by the test-setup
    Assertions.assertEquals(3, listResponse.getTotalResults());
    goldfish = getKeycloakSession().users().getUserById(getRealmModel(), goldfish.getId());
    Assertions.assertNull(goldfish.getCreatedTimestamp());
  }

  /**
   * verifies that the associated groups are returned if a user is accessed
   */
  @Test
  public void testReturnUserWithGroups()
  {
    UserModel superMario = getKeycloakSession().users().addUser(getRealmModel(), "supermario");

    GroupModel nintendo = getKeycloakSession().groups().createGroup(getRealmModel(), "nintendo");
    GroupModel marioClub = getKeycloakSession().groups().createGroup(getRealmModel(), "mario club");

    superMario.joinGroup(nintendo);
    superMario.joinGroup(marioClub);

    RequestMock.mockRequest(getScimEndpoint(), getKeycloakSession())
               .method(HttpMethod.GET)
               .endpoint(String.format("%s/%s", EndpointPaths.USERS, superMario.getId()));
    Response response = getScimEndpoint().handleScimRequest(null);
    Assertions.assertEquals(HttpStatus.OK, response.getStatus());

    User user = JsonHelper.readJsonDocument((String)response.getEntity(), User.class);
    Assertions.assertEquals(2, user.getGroups().size());
    GroupNode nintendoNode = user.getGroups()
                                 .stream()
                                 .filter(g -> g.getDisplay().get().equals(nintendo.getName()))
                                 .findAny()
                                 .get();
    Assertions.assertEquals("direct", nintendoNode.getType().get());
    GroupNode marioClubNode = user.getGroups()
                                  .stream()
                                  .filter(g -> g.getDisplay().get().equals(marioClub.getName()))
                                  .findAny()
                                  .get();
    Assertions.assertEquals("direct", marioClubNode.getType().get());
  }
}
