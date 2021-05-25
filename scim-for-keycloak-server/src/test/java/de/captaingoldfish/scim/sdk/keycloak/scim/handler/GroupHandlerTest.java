package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractScimEndpointTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.models.GroupModel;
import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;
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
    }
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
}
