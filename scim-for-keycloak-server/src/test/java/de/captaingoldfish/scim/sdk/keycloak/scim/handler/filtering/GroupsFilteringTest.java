package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.provider.Arguments;
import org.keycloak.models.GroupModel;
import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.GroupNode;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractScimEndpointTest;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;


/**
 * @author Pascal Knueppel
 * @since 19.12.2022
 */
public class GroupsFilteringTest extends AbstractScimEndpointTest
{

  /**
   * initialize test-class
   */
  @BeforeEach
  public void initialize()
  {
    // remove the predefined user because we do not want it within these tests
    getKeycloakSession().users().removeUser(getRealmModel(), getTestUser());
  }

  /**
   * make sure that various different filter expressions are successfully executed using three different users
   */
  @TestFactory
  public List<DynamicTest> testGroupFiltering()
  {
    CustomUser superMarioScim = JsonHelper.loadJsonDocument(USER_SUPER_MARIO, CustomUser.class);
    CustomUser donkeyKongScim = JsonHelper.loadJsonDocument(USER_DONKEY_KONG, CustomUser.class);
    CustomUser linkScim = JsonHelper.loadJsonDocument(USER_LINK, CustomUser.class);

    superMarioScim = createUser(superMarioScim);
    donkeyKongScim = createUser(donkeyKongScim);
    linkScim = createUser(linkScim);

    GroupModel adminGroup = getKeycloakSession().groups().createGroup(getRealmModel(), "admin");
    GroupModel userGroup = getKeycloakSession().groups().createGroup(getRealmModel(), "user");
    GroupModel moderatorGroup = getKeycloakSession().groups().createGroup(getRealmModel(), "moderator");
    GroupModel scimAdmin = getKeycloakSession().groups().createGroup(getRealmModel(), "scimAdmin");

    // let users join groups
    // link -> admin, user
    // mario -> moderator, user
    // donkey-kong -> user
    {
      UserModel marioModel = getKeycloakSession().users().getUserById(getRealmModel(), superMarioScim.getId().get());
      UserModel donkeyKongModel = getKeycloakSession().users()
                                                      .getUserById(getRealmModel(), donkeyKongScim.getId().get());
      UserModel linkModel = getKeycloakSession().users().getUserById(getRealmModel(), linkScim.getId().get());

      linkModel.joinGroup(adminGroup);

      marioModel.joinGroup(moderatorGroup);

      linkModel.joinGroup(userGroup);
      marioModel.joinGroup(userGroup);
      donkeyKongModel.joinGroup(userGroup);

      // add the groups also to the scim object for later comparison to check that the attributes are returned from
      // the endpoint
      superMarioScim.setGroups(Arrays.asList(GroupNode.builder().value(moderatorGroup.getName()).build(),
                                             GroupNode.builder().value(userGroup.getName()).build()));
      donkeyKongScim.setGroups(Arrays.asList(GroupNode.builder().value(userGroup.getName()).build()));
      linkScim.setGroups(Arrays.asList(GroupNode.builder().value(adminGroup.getName()).build(),
                                       GroupNode.builder().value(userGroup.getName()).build()));
    }

    return Stream.of(Arguments.arguments(null, new GroupModel[]{adminGroup, userGroup, moderatorGroup, scimAdmin}),
                     Arguments.arguments("displayName eq \"admin\"", new GroupModel[]{adminGroup}),
                     Arguments.arguments("members pr", new GroupModel[]{adminGroup, moderatorGroup, userGroup}),
                     Arguments.arguments("not(members pr)", new GroupModel[]{scimAdmin}),
                     Arguments.arguments(String.format("members.value eq \"%s\"", linkScim.getId().get()),
                                         new GroupModel[]{userGroup, adminGroup}),
                     Arguments.arguments(String.format("members.value eq \"%s\"", superMarioScim.getId().get()),
                                         new GroupModel[]{moderatorGroup, userGroup}),
                     Arguments.arguments(String.format("members.value eq \"%s\"", donkeyKongScim.getId().get()),
                                         new GroupModel[]{userGroup}),
                     Arguments.arguments(String.format("members.display eq \"%s\"", linkScim.getUserName().get()),
                                         new GroupModel[]{userGroup, adminGroup}),
                     Arguments.arguments(String.format("members.display eq \"%s\"", superMarioScim.getUserName().get()),
                                         new GroupModel[]{moderatorGroup, userGroup}),
                     Arguments.arguments(String.format("members.display eq \"%s\"", donkeyKongScim.getUserName().get()),
                                         new GroupModel[]{userGroup})
    //
    ).map(this::toFilterTest).collect(Collectors.toList());
  }

  /**
   * creates a dynamic test that initiates a filter-list-request at the users scim endpoint
   */
  private DynamicTest toFilterTest(Arguments arguments)
  {
    Object[] argumentArray = arguments.get();
    String filter = (String)argumentArray[0];
    GroupModel[] expectedGroups = (GroupModel[])(argumentArray.length == 2 ? argumentArray[1] : null);
    int expectedResults = expectedGroups == null ? 0 : expectedGroups.length;

    Assertions.assertEquals(expectedResults, expectedGroups == null ? 0 : expectedGroups.length);

    final String testName = String.format("filter: %s", filter);
    return DynamicTest.dynamicTest(testName, () -> {
      String encodedFilter = Optional.ofNullable(filter).map(f -> String.format("?filter=%s", encodeUrl(f))).orElse("");
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                                 .method(HttpMethod.GET)
                                                 .endpoint(EndpointPaths.GROUPS + encodedFilter)
                                                 .build();
      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.OK, response.getStatus());
      ListResponse<ScimObjectNode> listResponse = JsonHelper.readJsonDocument(response.readEntity(String.class),
                                                                              ListResponse.class);
      Assertions.assertEquals(1, listResponse.getStartIndex());
      Assertions.assertEquals(expectedResults,
                              listResponse.getTotalResults(),
                              listResponse.getListedResources()
                                          .stream()
                                          .map(objectNode -> objectNode.get(AttributeNames.RFC7643.DISPLAY_NAME)
                                                                       .textValue())
                                          .collect(Collectors.toList())
                                          .toString());
      Assertions.assertEquals(expectedResults, listResponse.getItemsPerPage());
      List<ScimObjectNode> resources = listResponse.getListedResources();
      Assertions.assertEquals(expectedResults, resources.size());

      if (expectedResults != 0)
      {
        Assertions.assertTrue(Arrays.stream(expectedGroups).allMatch(groupModel -> {
          return resources.stream()
                          .anyMatch(resource -> resource.get(AttributeNames.RFC7643.DISPLAY_NAME)
                                                        .textValue()
                                                        .equals(groupModel.getName()));
        }),
                              resources.stream()
                                       .map(resource -> resource.get(AttributeNames.RFC7643.DISPLAY_NAME).textValue())
                                       .collect(Collectors.toList())
                                       .toString());
      }
    });
  }
}
