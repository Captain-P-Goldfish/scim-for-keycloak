package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractScimEndpointTest;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 27.08.2020
 */
@Slf4j
public class GroupHandlerTest extends AbstractScimEndpointTest
{

  /**
   * will verify that a member is being removed from a group if no longer present in the members section
   *
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/54">
   *      https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/54 </a>
   */
  @Test
  public void testMembersAreRemovedFromGroup()
  {
    UserModel superMario = getKeycloakSession().users().addUser(getRealmModel(), "supermario");
    UserModel bowser = getKeycloakSession().users().addUser(getRealmModel(), "bowser");

    GroupModel nintendo = getKeycloakSession().groups().createGroup(getRealmModel(), "nintendo");
    GroupModel retroStudios = getKeycloakSession().groups().createGroup(getRealmModel(), "retro studios");
    GroupModel marioClub = getKeycloakSession().groups().createGroup(getRealmModel(), "mario club");


    {
      Member memberMario = Member.builder().value(superMario.getId()).type("User").build();
      Member memberBowser = Member.builder().value(bowser.getId()).type("User").build();
      Member memberRetroStudios = Member.builder().value(retroStudios.getId()).type("Group").build();
      Member memberMarioClub = Member.builder().value(marioClub.getId()).type("Group").build();

      PatchOpRequest patchOpRequest = new PatchOpRequest();
      List<PatchRequestOperation> operations = new ArrayList<>();
      operations.add(PatchRequestOperation.builder()
                                          .op(PatchOp.ADD)
                                          .path("members")
                                          .value(memberMario.toString())
                                          .build());
      operations.add(PatchRequestOperation.builder()
                                          .op(PatchOp.ADD)
                                          .path("members")
                                          .value(memberBowser.toString())
                                          .build());
      operations.add(PatchRequestOperation.builder()
                                          .op(PatchOp.ADD)
                                          .path("members")
                                          .value(memberRetroStudios.toString())
                                          .build());
      operations.add(PatchRequestOperation.builder()
                                          .op(PatchOp.ADD)
                                          .path("members")
                                          .value(memberMarioClub.toString())
                                          .build());
      patchOpRequest.setOperations(operations);

      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                                 .endpoint(EndpointPaths.GROUPS + "/" + nintendo.getId())
                                                 .method(HttpMethod.PATCH)
                                                 .requestBody(patchOpRequest.toString())
                                                 .build();

      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.OK, response.getStatus());

      Assertions.assertTrue(superMario.isMemberOf(nintendo));
      Assertions.assertTrue(bowser.isMemberOf(nintendo));
      Assertions.assertEquals(2, nintendo.getSubGroupsStream().count());
      Assertions.assertTrue(nintendo.getSubGroupsStream()
                                    .map(GroupModel::getName)
                                    .anyMatch(name -> name.equals(retroStudios.getName())));
      Assertions.assertTrue(nintendo.getSubGroupsStream()
                                    .map(GroupModel::getName)
                                    .anyMatch(name -> name.equals(marioClub.getName())));
      // check for last modified
      Assertions.assertNotNull(nintendo.getFirstAttribute(AttributeNames.RFC7643.LAST_MODIFIED));
    }

    // now remove bowser and mario club as member from groups
    {
      PatchOpRequest patchOpRequest = new PatchOpRequest();
      List<PatchRequestOperation> operations = new ArrayList<>();

      operations.add(PatchRequestOperation.builder()
                                          .op(PatchOp.REMOVE)
                                          .path("members[value eq \"" + bowser.getId() + "\"]")
                                          .build());
      operations.add(PatchRequestOperation.builder()
                                          .op(PatchOp.REMOVE)
                                          .path("members[value eq \"" + marioClub.getId() + "\"]")
                                          .build());
      patchOpRequest.setOperations(operations);
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                                 .endpoint(EndpointPaths.GROUPS + "/" + nintendo.getId())
                                                 .method(HttpMethod.PATCH)
                                                 .requestBody(patchOpRequest.toString())
                                                 .build();

      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.OK, response.getStatus());

      Assertions.assertTrue(superMario.isMemberOf(nintendo), "super mario must still be a member of group nintendo");
      Assertions.assertFalse(bowser.isMemberOf(nintendo),
                             "bowser should have been removed as member of group " + "nintendo");
      Assertions.assertTrue(nintendo.getSubGroupsStream()
                                    .map(GroupModel::getName)
                                    .anyMatch(name -> name.equals(retroStudios.getName())));
      Assertions.assertFalse(nintendo.getSubGroupsStream()
                                     .map(GroupModel::getName)
                                     .anyMatch(name -> name.equals(marioClub.getName())));
      // check for last modified
      Assertions.assertNotNull(nintendo.getFirstAttribute(AttributeNames.RFC7643.LAST_MODIFIED));
    }
  }

  /**
   * this test will use the ref-attribute to set the resource-member link for a group and a member and will
   * check that the relationship is correctly setup
   */
  @Test
  public void testCreateGroupWithUserAndGroupMembersOnReferenceBase()
  {
    // members
    UserModel superMario = getKeycloakSession().users().addUser(getRealmModel(), "supermario");
    GroupModel retroStudios = getKeycloakSession().groups().createGroup(getRealmModel(), "retro studios");

    Group nintendo = Group.builder()
                          .displayName("nintendo")
                          .members(Arrays.asList(Member.builder()
                                                       .value(superMario.getId())
                                                       .ref(String.format("http://localhost/scim/v2%s/%s",
                                                                          EndpointPaths.USERS,
                                                                          superMario.getId()))
                                                       .build(),
                                                 Member.builder()
                                                       .value(retroStudios.getId())
                                                       .ref(String.format("http://localhost/scim/v2%s/%s",
                                                                          EndpointPaths.GROUPS,
                                                                          retroStudios.getId()))
                                                       .build()))
                          .build();

    Assertions.assertTrue(nintendo.getMembers().stream().anyMatch(member -> !member.getType().isPresent()));

    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .endpoint(EndpointPaths.GROUPS)
                                               .method(HttpMethod.POST)
                                               .requestBody(nintendo.toString())
                                               .build();

    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());

    Group createdGroup = JsonHelper.readJsonDocument((String)response.getEntity(), Group.class);
    GroupModel groupModel = getKeycloakSession().groups().getGroupById(getRealmModel(), createdGroup.getId().get());

    Assertions.assertTrue(groupModel.getSubGroupsStream().anyMatch(g -> g.getId().equals(retroStudios.getId())));
    Assertions.assertTrue(superMario.isMemberOf(groupModel));
    String created = groupModel.getFirstAttribute(AttributeNames.RFC7643.CREATED);
    Assertions.assertNotNull(created);
    Assertions.assertEquals(created, groupModel.getFirstAttribute(AttributeNames.RFC7643.LAST_MODIFIED));
  }

  /**
   * this test will verify that the last modified gets updated after a group was modified
   */
  @SneakyThrows
  @Test
  public void testLastModifiedIsChangedAfterUpdate()
  {
    // members
    Group nintendo = Group.builder().displayName("nintendo").build();

    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .endpoint(EndpointPaths.GROUPS)
                                               .method(HttpMethod.POST)
                                               .requestBody(nintendo.toString())
                                               .build();

    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());

    Group createdGroup = JsonHelper.readJsonDocument((String)response.getEntity(), Group.class);
    GroupModel groupModel = getKeycloakSession().groups().getGroupById(getRealmModel(), createdGroup.getId().get());

    String created = groupModel.getFirstAttribute(AttributeNames.RFC7643.CREATED);
    Assertions.assertNotNull(created);
    Assertions.assertEquals(created, groupModel.getFirstAttribute(AttributeNames.RFC7643.LAST_MODIFIED));


    Group groupToUpdate = Group.builder().displayName("newCompanyName").build();
    request = RequestBuilder.builder(getScimEndpoint())
                            .endpoint(String.format("%s/%s", EndpointPaths.GROUPS, createdGroup.getId().get()))
                            .method(HttpMethod.PUT)
                            .requestBody(groupToUpdate.toString())
                            .build();
    Thread.sleep(1);
    Response updateResponse = getScimEndpoint().handleScimRequest(request);
    Group updatedGroup = JsonHelper.readJsonDocument((String)updateResponse.getEntity(), Group.class);
    Assertions.assertEquals(created, groupModel.getFirstAttribute(AttributeNames.RFC7643.CREATED));
    Assertions.assertNotEquals(created, groupModel.getFirstAttribute(AttributeNames.RFC7643.LAST_MODIFIED));
    Assertions.assertEquals(created, updatedGroup.getMeta().flatMap(Meta::getCreated).map(Instant::toString).get());
    Assertions.assertNotEquals(created,
                               updatedGroup.getMeta().flatMap(Meta::getLastModified).map(Instant::toString).get());
  }

  /**
   * will verify that creating a group with a none existing member causes a
   * {@link de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException}
   *
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/55">
   *      https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/55</a>
   */
  @ParameterizedTest
  @ValueSource(strings = {"User", "Group"})
  public void testCreateGroupWithNonExistingMember(String type)
  {
    final String notExistingId = UUID.randomUUID().toString();
    Group nintendo = Group.builder()
                          .displayName("nintendo")
                          .members(Arrays.asList(Member.builder().value(notExistingId).type(type).build()))
                          .build();

    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .endpoint(EndpointPaths.GROUPS)
                                               .method(HttpMethod.POST)
                                               .requestBody(nintendo.toString())
                                               .build();

    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    log.warn((String)response.getEntity());
    ErrorResponse errorResponse = JsonHelper.readJsonDocument((String)response.getEntity(), ErrorResponse.class);
    Assertions.assertEquals(String.format("%s with id '%s' does not exist", type, notExistingId),
                            errorResponse.getDetail().get());
  }

  /**
   * verifies that groups can be created if their names are prefixes of other group names
   *
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/56">
   *      https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/56</a>
   */
  @Test
  public void testCreateGroupWithSimiliarNames()
  {
    final String prefixName = "groupBremen";
    getKeycloakSession().groups().createGroup(getRealmModel(), prefixName + "Remove");

    Group nintendo = Group.builder().displayName(prefixName).build();
    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .endpoint(EndpointPaths.GROUPS)
                                               .method(HttpMethod.POST)
                                               .requestBody(nintendo.toString())
                                               .build();

    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());
  }

  /**
   * verifies that a group with a duplicate name cannot be created
   *
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/56">
   *      https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/56</a>
   */
  @Test
  public void testCreateGroupWithDuplicateName()
  {
    GroupModel groupBremen = getKeycloakSession().groups().createGroup(getRealmModel(), "groupBremen");

    Group nintendo = Group.builder().displayName(groupBremen.getName()).build();
    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .endpoint(EndpointPaths.GROUPS)
                                               .method(HttpMethod.POST)
                                               .requestBody(nintendo.toString())
                                               .build();

    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.CONFLICT, response.getStatus());
  }

  /**
   * verifies that a group is created and that an admin event is correctly stored
   */
  @Test
  public void testAdminEventOnGroupCreated()
  {
    Group goldfish = Group.builder().displayName("goldfish").build();
    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .endpoint(EndpointPaths.GROUPS)
                                               .method(HttpMethod.POST)
                                               .requestBody(goldfish.toString())
                                               .build();

    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());

    Group createdGroup = JsonHelper.readJsonDocument((String)response.getEntity(), Group.class);
    // check for created admin event
    {
      List<AdminEvent> adminEventList = getAdminEventStoreProvider().createAdminQuery()
                                                                    .getResultStream()
                                                                    .collect(Collectors.toList());
      Assertions.assertEquals(1, adminEventList.size());
      AdminEvent adminEvent = adminEventList.get(0);
      Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      Assertions.assertEquals("groups/" + createdGroup.getId().get(), adminEvent.getResourcePath());
      Assertions.assertEquals(OperationType.CREATE, adminEvent.getOperationType());
      Assertions.assertEquals(ResourceType.GROUP, adminEvent.getResourceType());
      // equalize the two objects by modifying the meta-attribute. The meta-attribute is not identical because the
      // schema-validation is modifying the meta-attribute when evaluating the response
      {
        createdGroup.getMeta().get().setResourceType(null);
        createdGroup.getMeta().get().setLocation(null);
      }
      Assertions.assertEquals(createdGroup, JsonHelper.readJsonDocument(adminEvent.getRepresentation(), Group.class));
    }
  }

  /**
   * verifies that for an admin event is triggered for each group add member operation.
   */
  @Test
  public void testAdminEventsOnGroupCreatedWithMembers()
  {
    UserModel superMario = getKeycloakSession().users().addUser(getRealmModel(), "supermario");
    UserModel bowser = getKeycloakSession().users().addUser(getRealmModel(), "bowser");

    GroupModel marioClub = getKeycloakSession().groups().createGroup(getRealmModel(), "mario club");
    GroupModel luigiClub = getKeycloakSession().groups().createGroup(getRealmModel(), "luigi club");

    List<Member> groupMembers = Arrays.asList(Member.builder()
                                                    .type(ResourceTypeNames.USER)
                                                    .value(superMario.getId())
                                                    .build(),
                                              Member.builder()
                                                    .type(ResourceTypeNames.USER)
                                                    .value(bowser.getId())
                                                    .build(),
                                              Member.builder()
                                                    .type(ResourceTypeNames.GROUPS)
                                                    .value(marioClub.getId())
                                                    .build(),
                                              Member.builder()
                                                    .type(ResourceTypeNames.GROUPS)
                                                    .value(luigiClub.getId())
                                                    .build());

    Group nintendo = Group.builder().displayName("nintendo").members(groupMembers).build();
    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .endpoint(EndpointPaths.GROUPS)
                                               .method(HttpMethod.POST)
                                               .requestBody(nintendo.toString())
                                               .build();

    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());

    Group createdGroup = JsonHelper.readJsonDocument((String)response.getEntity(), Group.class);
    GroupModel nintendoGroupModel = getKeycloakSession().groups()
                                                        .getGroupById(getRealmModel(), createdGroup.getId().get());

    Assertions.assertTrue(superMario.isMemberOf(nintendoGroupModel));
    Assertions.assertTrue(bowser.isMemberOf(nintendoGroupModel));
    Assertions.assertTrue(nintendoGroupModel.getSubGroupsStream().anyMatch(g -> g.getId().equals(marioClub.getId())));
    Assertions.assertTrue(nintendoGroupModel.getSubGroupsStream().anyMatch(g -> g.getId().equals(luigiClub.getId())));

    List<AdminEvent> adminEventList = getAdminEventStoreProvider().createAdminQuery()
                                                                  .getResultStream()
                                                                  .collect(Collectors.toList());
    Assertions.assertEquals(5, adminEventList.size());
    // check for created admin event that the group was created
    {
      AdminEvent adminEvent = adminEventList.stream()
                                            .filter(event -> event.getResourceType().equals(ResourceType.GROUP))
                                            .findAny()
                                            .get();
      Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      MatcherAssert.assertThat(adminEvent.getResourcePath(), Matchers.endsWith("groups/" + createdGroup.getId().get()));
      Assertions.assertEquals(OperationType.CREATE, adminEvent.getOperationType());
      Assertions.assertEquals(ResourceType.GROUP, adminEvent.getResourceType());
      // equalize the two objects by modifying the meta-attribute. The meta-attribute is not identical because the
      // schema-validation is modifying the meta-attribute when evaluating the response
      {
        createdGroup.getMeta().get().setResourceType(null);
        createdGroup.getMeta().get().setLocation(null);
      }
      List<Member> membersWithRefRemoved = createdGroup.getMembers()
                                                       .stream()
                                                       .peek(member -> member.setRef(null))
                                                       .collect(Collectors.toList());
      createdGroup.setMembers(membersWithRefRemoved);
      Assertions.assertEquals(createdGroup, JsonHelper.readJsonDocument(adminEvent.getRepresentation(), Group.class));
    }

    // check for admin event that the user supermario was added as a group member
    {
      AdminEvent adminEvent = adminEventList.stream()
                                            .filter(event -> event.getResourcePath()
                                                                  .equals(String.format("users/%s/groups/%s",
                                                                                        superMario.getId(),
                                                                                        nintendoGroupModel.getId())))
                                            .findAny()
                                            .get();
      Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      Assertions.assertEquals(OperationType.CREATE, adminEvent.getOperationType());
      Assertions.assertEquals(ResourceType.GROUP_MEMBERSHIP, adminEvent.getResourceType());
      Assertions.assertEquals(Group.builder()
                                   .id(nintendoGroupModel.getId())
                                   .displayName(nintendoGroupModel.getName())
                                   .members(Collections.singletonList(Member.builder()
                                                                            .value(superMario.getId())
                                                                            .type(ResourceTypeNames.USER)
                                                                            .display(superMario.getUsername())
                                                                            .build()))
                                   .build(),
                              JsonHelper.readJsonDocument(adminEvent.getRepresentation(), Group.class));
    }

    // check for admin event that the user bowser was added as a group member
    {
      AdminEvent adminEvent = adminEventList.stream()
                                            .filter(event -> event.getResourcePath()
                                                                  .equals(String.format("users/%s/groups/%s",
                                                                                        bowser.getId(),
                                                                                        nintendoGroupModel.getId())))
                                            .findAny()
                                            .get();
      Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      Assertions.assertEquals(OperationType.CREATE, adminEvent.getOperationType());
      Assertions.assertEquals(ResourceType.GROUP_MEMBERSHIP, adminEvent.getResourceType());
      Assertions.assertEquals(Group.builder()
                                   .id(nintendoGroupModel.getId())
                                   .displayName(nintendoGroupModel.getName())
                                   .members(Collections.singletonList(Member.builder()
                                                                            .value(bowser.getId())
                                                                            .type(ResourceTypeNames.USER)
                                                                            .display(bowser.getUsername())
                                                                            .build()))
                                   .build(),
                              JsonHelper.readJsonDocument(adminEvent.getRepresentation(), Group.class));
    }

    // check for admin event that the group mario-club was added as a group member
    {
      AdminEvent adminEvent = adminEventList.stream()
                                            .filter(event -> event.getResourcePath()
                                                                  .equals(String.format("groups/%s/groups/%s",
                                                                                        marioClub.getId(),
                                                                                        nintendoGroupModel.getId())))
                                            .findAny()
                                            .get();
      Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      Assertions.assertEquals(OperationType.CREATE, adminEvent.getOperationType());
      Assertions.assertEquals(ResourceType.GROUP_MEMBERSHIP, adminEvent.getResourceType());
      Assertions.assertEquals(Group.builder()
                                   .id(nintendoGroupModel.getId())
                                   .displayName(nintendoGroupModel.getName())
                                   .members(Collections.singletonList(Member.builder()
                                                                            .value(marioClub.getId())
                                                                            .type(ResourceTypeNames.GROUPS)
                                                                            .display(marioClub.getName())
                                                                            .build()))
                                   .build(),
                              JsonHelper.readJsonDocument(adminEvent.getRepresentation(), Group.class));
    }

    // check for admin event that the group luigi-club was added as a group member
    {
      AdminEvent adminEvent = adminEventList.stream()
                                            .filter(event -> event.getResourcePath()
                                                                  .equals(String.format("groups/%s/groups/%s",
                                                                                        luigiClub.getId(),
                                                                                        nintendoGroupModel.getId())))
                                            .findAny()
                                            .get();
      Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      Assertions.assertEquals(OperationType.CREATE, adminEvent.getOperationType());
      Assertions.assertEquals(ResourceType.GROUP_MEMBERSHIP, adminEvent.getResourceType());
      Assertions.assertEquals(Group.builder()
                                   .id(nintendoGroupModel.getId())
                                   .displayName(nintendoGroupModel.getName())
                                   .members(Collections.singletonList(Member.builder()
                                                                            .value(luigiClub.getId())
                                                                            .type(ResourceTypeNames.GROUPS)
                                                                            .display(luigiClub.getName())
                                                                            .build()))
                                   .build(),
                              JsonHelper.readJsonDocument(adminEvent.getRepresentation(), Group.class));
    }
  }

  /**
   * verifies that an admin event is stored if a group is deleted
   */
  @Test
  public void testAdminEventOnGroupDeleted()
  {
    GroupModel nintendo = getKeycloakSession().groups().createGroup(getRealmModel(), "nintendo");

    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .endpoint(EndpointPaths.GROUPS + "/" + nintendo.getId())
                                               .method(HttpMethod.DELETE)
                                               .build();

    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatus());

    // check for created admin event
    {
      List<AdminEvent> adminEventList = getAdminEventStoreProvider().createAdminQuery()
                                                                    .getResultStream()
                                                                    .collect(Collectors.toList());
      Assertions.assertEquals(1, adminEventList.size());
      AdminEvent adminEvent = adminEventList.get(0);
      Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      Assertions.assertEquals("groups/" + nintendo.getId(), adminEvent.getResourcePath());
      Assertions.assertEquals(OperationType.DELETE, adminEvent.getOperationType());
      Assertions.assertEquals(ResourceType.GROUP, adminEvent.getResourceType());
      Assertions.assertEquals(Group.builder().id(nintendo.getId()).displayName(nintendo.getName()).build(),
                              JsonHelper.readJsonDocument(adminEvent.getRepresentation(), Group.class));
    }
  }

  /**
   * verifies that an admin event is stored if a group membership is deleted. Additionally the a second
   * admin-event must be stored that tells us that the group was updated
   */
  @Test
  public void testAdminEventOnGroupRemoved()
  {
    UserModel superMario = getKeycloakSession().users().addUser(getRealmModel(), "supermario");
    UserModel bowser = getKeycloakSession().users().addUser(getRealmModel(), "bowser");

    GroupModel marioClub = getKeycloakSession().groups().createGroup(getRealmModel(), "mario club");
    GroupModel luigiClub = getKeycloakSession().groups().createGroup(getRealmModel(), "luigi club");
    GroupModel nintendo = getKeycloakSession().groups().createGroup(getRealmModel(), "nintendo");

    nintendo.addChild(marioClub);
    nintendo.addChild(luigiClub);
    superMario.joinGroup(nintendo);
    bowser.joinGroup(nintendo);

    List<Member> groupMembers = Arrays.asList(Member.builder()
                                                    .type(ResourceTypeNames.USER)
                                                    .value(superMario.getId())
                                                    .build(),
                                              Member.builder()
                                                    .type(ResourceTypeNames.GROUPS)
                                                    .value(marioClub.getId())
                                                    .build());

    Group nintendoGroup = Group.builder().displayName("nintendo").members(groupMembers).build();
    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .endpoint(EndpointPaths.GROUPS + "/" + nintendo.getId())
                                               .method(HttpMethod.PUT)
                                               .requestBody(nintendoGroup.toString())
                                               .build();

    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.OK, response.getStatus());

    Group updatedGroup = JsonHelper.readJsonDocument((String)response.getEntity(), Group.class);
    List<AdminEvent> adminEventList = getAdminEventStoreProvider().createAdminQuery()
                                                                  .getResultStream()
                                                                  .collect(Collectors.toList());
    Assertions.assertEquals(3, adminEventList.size());
    // check for created admin event that the group was created
    {
      AdminEvent adminEvent = adminEventList.stream()
                                            .filter(event -> event.getOperationType().equals(OperationType.UPDATE))
                                            .findAny()
                                            .get();
      Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      MatcherAssert.assertThat(adminEvent.getResourcePath(), Matchers.endsWith("groups/" + updatedGroup.getId().get()));
      Assertions.assertEquals(OperationType.UPDATE, adminEvent.getOperationType());
      Assertions.assertEquals(ResourceType.GROUP, adminEvent.getResourceType());
      // equalize the two objects by modifying the meta-attribute. The meta-attribute is not identical because the
      // schema-validation is modifying the meta-attribute when evaluating the response
      {
        updatedGroup.getMeta().get().setResourceType(null);
        updatedGroup.getMeta().get().setLocation(null);
      }
      List<Member> membersWithRefRemoved = updatedGroup.getMembers()
                                                       .stream()
                                                       .peek(member -> member.setRef(null))
                                                       .collect(Collectors.toList());
      updatedGroup.setMembers(membersWithRefRemoved);
      Assertions.assertEquals(updatedGroup, JsonHelper.readJsonDocument(adminEvent.getRepresentation(), Group.class));
    }

    // check for admin event that the user bowser was remove as a group member
    {
      AdminEvent adminEvent = adminEventList.stream()
                                            .filter(event -> event.getResourcePath()
                                                                  .equals(String.format("users/%s/groups/%s",
                                                                                        bowser.getId(),
                                                                                        nintendo.getId())))
                                            .findAny()
                                            .get();
      Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      Assertions.assertEquals(OperationType.DELETE, adminEvent.getOperationType());
      Assertions.assertEquals(ResourceType.GROUP_MEMBERSHIP, adminEvent.getResourceType());
      Assertions.assertEquals(Group.builder()
                                   .id(nintendo.getId())
                                   .displayName(nintendo.getName())
                                   .members(Collections.singletonList(Member.builder()
                                                                            .value(bowser.getId())
                                                                            .type(ResourceTypeNames.USER)
                                                                            .display(bowser.getUsername())
                                                                            .build()))
                                   .build(),
                              JsonHelper.readJsonDocument(adminEvent.getRepresentation(), Group.class));
    }

    // check for admin event that the group luigi-club was remove as a group member
    {
      AdminEvent adminEvent = adminEventList.stream()
                                            .filter(event -> event.getResourcePath()
                                                                  .equals(String.format("groups/%s/groups/%s",
                                                                                        luigiClub.getId(),
                                                                                        nintendo.getId())))
                                            .findAny()
                                            .get();
      Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
      Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
      Assertions.assertEquals(OperationType.DELETE, adminEvent.getOperationType());
      Assertions.assertEquals(ResourceType.GROUP_MEMBERSHIP, adminEvent.getResourceType());
      Assertions.assertEquals(Group.builder()
                                   .id(nintendo.getId())
                                   .displayName(nintendo.getName())
                                   .members(Collections.singletonList(Member.builder()
                                                                            .value(luigiClub.getId())
                                                                            .type(ResourceTypeNames.GROUPS)
                                                                            .display(luigiClub.getName())
                                                                            .build()))
                                   .build(),
                              JsonHelper.readJsonDocument(adminEvent.getRepresentation(), Group.class));
    }
  }

  /**
   * will add a new user member to a group by using the patch add operation:
   *
   * <pre>
   * {
   *     "schemas": [
   *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *     ],
   *     "Operations": [
   *         {
   *             "op": "add",
   *             "path": "members",
   *             "value": [
   *                 {
   *                     "value": "${userId}"
   *                 }
   *             ]
   *         }
   *     ]
   * }
   * </pre>
   */
  @Test
  public void testAddUserMemberToGroupWithPatch()
  {
    UserModel superMario = getKeycloakSession().users().addUser(getRealmModel(), "supermario");
    UserModel bowser = getKeycloakSession().users().addUser(getRealmModel(), "bowser");

    GroupModel marioClub = getKeycloakSession().groups().createGroup(getRealmModel(), "mario club");

    superMario.joinGroup(marioClub);

    List<PatchRequestOperation> operations = new ArrayList<>();
    operations.add(PatchRequestOperation.builder()
                                        .op(PatchOp.ADD)
                                        .path("members")
                                        .valueNode(Member.builder().value(bowser.getId()).build())
                                        .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    log.warn(patchOpRequest.toPrettyString());

    HttpServletRequest servletRequest = RequestBuilder.builder(getScimEndpoint())
                                                      .method(HttpMethod.PATCH)
                                                      .endpoint(EndpointPaths.GROUPS + "/" + marioClub.getId())
                                                      .requestBody(patchOpRequest.toString())
                                                      .build();
    Response response = getScimEndpoint().handleScimRequest(servletRequest);
    Assertions.assertEquals(HttpStatus.OK, response.getStatus());

    Group group = JsonHelper.readJsonDocument((String)response.getEntity(), Group.class);

    Assertions.assertEquals(2, group.getMembers().size());
  }
}
