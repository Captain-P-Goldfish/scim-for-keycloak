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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.provider.Arguments;
import org.keycloak.models.GroupModel;
import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.GroupNode;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractScimEndpointTest;
import de.captaingoldfish.scim.sdk.keycloak.scim.helper.UserComparator;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 12.12.2022
 */
@Slf4j
public class UserFilteringTest extends AbstractScimEndpointTest
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
   * this test makes sure that it is not possible to filter for user passwords
   */
  @Test
  public void testFilterForPassword()
  {
    String encodedFilter = String.format("?filter=%s", encodeUrl("password eq \"123456\""));
    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .method(HttpMethod.GET)
                                               .endpoint(EndpointPaths.USERS + encodedFilter)
                                               .build();
    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
    ErrorResponse errorResponse = JsonHelper.readJsonDocument((String)response.getEntity(), ErrorResponse.class);
    Assertions.assertEquals("Illegal filter-attribute found 'urn:ietf:params:scim:schemas:core:2.0:User:password'",
                            errorResponse.getDetail().get());
  }

  /**
   * make sure that various different filter expressions are successfully executed using three different users
   */
  @TestFactory
  public List<DynamicTest> testUserFiltering()
  {
    CustomUser superMarioScim = JsonHelper.loadJsonDocument(USER_SUPER_MARIO, CustomUser.class);
    CustomUser donkeyKongScim = JsonHelper.loadJsonDocument(USER_DONKEY_KONG, CustomUser.class);
    CustomUser linkScim = JsonHelper.loadJsonDocument(USER_LINK, CustomUser.class);
    CustomUser zeldaScim = CustomUser.builder().userName("zelda").build();

    superMarioScim = createUser(superMarioScim);
    donkeyKongScim = createUser(donkeyKongScim);
    linkScim = createUser(linkScim);
    zeldaScim = createUser(zeldaScim);

    GroupModel adminGroup = getKeycloakSession().groups().createGroup(getRealmModel(), "admin");
    GroupModel userGroup = getKeycloakSession().groups().createGroup(getRealmModel(), "user");
    GroupModel moderatorGroup = getKeycloakSession().groups().createGroup(getRealmModel(), "moderator");

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

    return Stream.of(Arguments.arguments(null, new CustomUser[]{superMarioScim, donkeyKongScim, linkScim, zeldaScim}),
                     Arguments.arguments(String.format("username eq " + "\"%s\"", superMarioScim.getUserName().get()),
                                         new CustomUser[]{superMarioScim}),
                     Arguments.arguments(String.format("username eq " + "\"%s\"", donkeyKongScim.getUserName().get()),
                                         new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments(String.format("username eq " + "\"%s\"", linkScim.getUserName().get()),
                                         new CustomUser[]{linkScim}),
                     Arguments.arguments("externalid co " + "\"c\"", new CustomUser[]{donkeyKongScim, linkScim}),
                     Arguments.arguments("externalid ew " + "\"2\"", new CustomUser[]{donkeyKongScim, superMarioScim}),
                     Arguments.arguments("active eq true", new CustomUser[]{superMarioScim, linkScim}),
                     Arguments.arguments("active eq false", new CustomUser[]{donkeyKongScim, zeldaScim}),
                     Arguments.arguments("name.formatted eq \"Donkey Kong\"", new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments("name.givenname eq \"Link\"", new CustomUser[]{linkScim}),
                     Arguments.arguments("name.familyName eq \"super\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("name.middleName eq \"-\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("name.middleName pr", new CustomUser[]{superMarioScim, donkeyKongScim}),
                     Arguments.arguments("not (name.middleName pr)", new CustomUser[]{linkScim, zeldaScim}),
                     Arguments.arguments("name pr", new CustomUser[]{donkeyKongScim, superMarioScim, linkScim}),
                     Arguments.arguments("not (name pr)", new CustomUser[]{zeldaScim}),
                     Arguments.arguments("name.honorificPrefix eq \"GG\"", new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments("name.honorificSuffix eq \"Mushroom\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("displayName co \"k\"", new CustomUser[]{donkeyKongScim, linkScim}),
                     Arguments.arguments("nickname co \"k\"", new CustomUser[]{donkeyKongScim, linkScim}),
                     Arguments.arguments("profileurl ew \"Mario\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("usertype eq \"plumber\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("preferredLanguage ne \"de\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("locale sw \"en\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("timezone sw \"America\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("username eq \"donkey-kong\" or username eq \"link\"",
                                         new CustomUser[]{donkeyKongScim, linkScim}),
                     Arguments.arguments("username co \"n\" and username co \"k\"",
                                         new CustomUser[]{donkeyKongScim, linkScim}),
                     Arguments.arguments("username co \"i\" and (username co \"k\" or username co \"d\")",
                                         new CustomUser[]{linkScim}),
                     Arguments.arguments("username co \"i\" and username co \"k\" or username co \"d\"",
                                         new CustomUser[]{donkeyKongScim, linkScim, zeldaScim}),
                     Arguments.arguments("username co \"i\" and not (username co \"k\" or username co \"d\")",
                                         new CustomUser[]{superMarioScim}),
                     Arguments.arguments("meta.created gt \"" + superMarioScim.getMeta().flatMap(Meta::getCreated).get()
                                         + "\"",
                                         new CustomUser[]{donkeyKongScim, linkScim, zeldaScim}),
                     Arguments.arguments("meta.created le \"" + superMarioScim.getMeta().flatMap(Meta::getCreated).get()
                                         + "\"",
                                         new CustomUser[]{superMarioScim}),
                     Arguments.arguments("meta.created eq \"" + superMarioScim.getMeta().flatMap(Meta::getCreated).get()
                                         + "\"",
                                         new CustomUser[]{superMarioScim}),
                     Arguments.arguments("meta.created lt \"" + superMarioScim.getMeta().flatMap(Meta::getCreated).get()
                                         + "\"",
                                         new CustomUser[]{}),
                     Arguments.arguments("meta.created lt \"" + donkeyKongScim.getMeta().flatMap(Meta::getCreated).get()
                                         + "\"",
                                         new CustomUser[]{superMarioScim}),
                     Arguments.arguments("meta.created le \"" + donkeyKongScim.getMeta().flatMap(Meta::getCreated).get()
                                         + "\"",
                                         new CustomUser[]{superMarioScim, donkeyKongScim}),
                     Arguments.arguments("meta.created gt \"" + donkeyKongScim.getMeta().flatMap(Meta::getCreated).get()
                                         + "\"",
                                         new CustomUser[]{linkScim, zeldaScim}),
                     Arguments.arguments("meta.created ge \"" + donkeyKongScim.getMeta().flatMap(Meta::getCreated).get()
                                         + "\"",
                                         new CustomUser[]{linkScim, donkeyKongScim, zeldaScim}),
                     Arguments.arguments("meta.lastmodified eq \""
                                         + linkScim.getMeta().flatMap(Meta::getLastModified).get() + "\"",
                                         new CustomUser[]{linkScim}),
                     Arguments.arguments("meta.lastmodified lt \""
                                         + linkScim.getMeta().flatMap(Meta::getLastModified).get() + "\"",
                                         new CustomUser[]{superMarioScim, donkeyKongScim}),
                     Arguments.arguments("meta.lastmodified gt \""
                                         + donkeyKongScim.getMeta().flatMap(Meta::getLastModified).get() + "\"",
                                         new CustomUser[]{linkScim, zeldaScim}),
                     Arguments.arguments("meta.lastmodified eq \""
                                         + donkeyKongScim.getMeta().flatMap(Meta::getLastModified).get() + "\"",
                                         new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments("meta.lastmodified lt \""
                                         + donkeyKongScim.getMeta().flatMap(Meta::getLastModified).get() + "\"",
                                         new CustomUser[]{superMarioScim}),
                     Arguments.arguments("meta.lastmodified le \""
                                         + donkeyKongScim.getMeta().flatMap(Meta::getLastModified).get() + "\"",
                                         new CustomUser[]{superMarioScim, donkeyKongScim}),

                     Arguments.arguments("addresses.streetAddress co \"jungle\"", new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments("addresses.locality eq \"worpswede\"", new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments("addresses.region eq \"bremen\"", new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments("addresses.postalCode eq \"546987\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("addresses.country eq \"hyrule\"", new CustomUser[]{linkScim}),
                     Arguments.arguments("addresses.type eq \"home\"", new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments("addresses.primary eq true", new CustomUser[]{superMarioScim, linkScim}),

                     Arguments.arguments("x509Certificates.value eq \"MII...2\"", new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments("x509Certificates.value sw \"MII...\"",
                                         new CustomUser[]{donkeyKongScim, superMarioScim, linkScim}),
                     Arguments.arguments("x509Certificates.display eq \"****\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("x509Certificates.type pr", new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments("x509Certificates.primary eq true",
                                         new CustomUser[]{superMarioScim, linkScim}),

                     Arguments.arguments("emails.type eq \"work\"", new CustomUser[]{donkeyKongScim, linkScim}),
                     Arguments.arguments("emails.type eq \"home\"", new CustomUser[]{donkeyKongScim, superMarioScim}),
                     Arguments.arguments("emails.type eq \"castle\"", new CustomUser[]{linkScim}),
                     Arguments.arguments("emails.type eq \"cabin\" or emails.type eq \"castle\"",
                                         new CustomUser[]{superMarioScim, linkScim}),
                     Arguments.arguments("emails.value eq \"donkey-kong@nintendo.de\"",
                                         new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments("emails.value eq \"mario@home.net\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("emails.value eq \"link@hyrule.de\"", new CustomUser[]{linkScim}),
                     Arguments.arguments("emails.type eq \"home\"", new CustomUser[]{superMarioScim, donkeyKongScim}),
                     Arguments.arguments("emails.primary eq true", new CustomUser[]{linkScim, donkeyKongScim}),

                     Arguments.arguments("entitlements.value eq \"donkey-ent-1\"", new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments("entitlements.display eq \"---\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("entitlements.type eq \"home\"", new CustomUser[]{linkScim}),
                     Arguments.arguments("entitlements.primary eq true", new CustomUser[]{linkScim}),

                     Arguments.arguments("ims.value eq \"donkey@kong\"", new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments("ims.display eq \"hello\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("ims.type eq \"work\"", new CustomUser[]{linkScim}),
                     Arguments.arguments("ims.primary eq true", new CustomUser[]{donkeyKongScim}),

                     Arguments.arguments("phoneNumbers.value eq \"987789987789\"", new CustomUser[]{donkeyKongScim}),
                     Arguments.arguments("phoneNumbers.display eq \"*\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("phoneNumbers.type eq \"work\"", new CustomUser[]{linkScim}),
                     Arguments.arguments("phoneNumbers.primary eq true", new CustomUser[]{donkeyKongScim, linkScim}),

                     Arguments.arguments("photos.value eq \"super-mario.png\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("photos.display eq \"hello world\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("photos.type eq \"home\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("photos.primary eq true", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("photos.value eq \"super-mario.png\" and photos.type eq \"home\"",
                                         new CustomUser[]{superMarioScim}),

                     Arguments.arguments("roles.value eq \"FacultyMember\"",
                                         new CustomUser[]{donkeyKongScim, superMarioScim}),
                     Arguments.arguments("roles.display eq \"faculty\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("roles.type eq \"work\"", new CustomUser[]{donkeyKongScim, linkScim}),
                     Arguments.arguments("roles.primary eq true", new CustomUser[]{linkScim}),
                     Arguments.arguments("roles pr", new CustomUser[]{donkeyKongScim, superMarioScim, linkScim}),
                     Arguments.arguments("not(roles pr)", new CustomUser[]{zeldaScim}),

                     Arguments.arguments("groups.value eq \"admin\"", new CustomUser[]{linkScim}),
                     Arguments.arguments("groups.value eq \"moderator\"", new CustomUser[]{superMarioScim}),
                     Arguments.arguments("groups.value eq \"user\"",
                                         new CustomUser[]{superMarioScim, donkeyKongScim, linkScim}),

                     Arguments.arguments("countries eq \"USA\"", new CustomUser[]{linkScim}),
                     Arguments.arguments("countries eq \"italy\"", new CustomUser[]{donkeyKongScim, superMarioScim}),
                     Arguments.arguments("countries eq \"japan\"", new CustomUser[]{donkeyKongScim, linkScim}),
                     Arguments.arguments("businessLine eq \"3\"", new CustomUser[]{donkeyKongScim, linkScim}),
                     Arguments.arguments("businessLine eq \"2\"",
                                         new CustomUser[]{donkeyKongScim, superMarioScim, linkScim}),
                     Arguments.arguments("businessLine eq \"5\"", new CustomUser[]{linkScim})
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
    CustomUser[] expectedUsers = (CustomUser[])(argumentArray.length == 2 ? argumentArray[1] : null);
    int expectedResults = expectedUsers == null ? 0 : expectedUsers.length;

    Assertions.assertEquals(expectedResults, expectedUsers == null ? 0 : expectedUsers.length);

    final String testName = String.format("filter: %s", filter);
    return DynamicTest.dynamicTest(testName, () -> {
      String encodedFilter = Optional.ofNullable(filter).map(f -> String.format("?filter=%s", encodeUrl(f))).orElse("");
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                                 .method(HttpMethod.GET)
                                                 .endpoint(EndpointPaths.USERS + encodedFilter)
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
                                          .map(objectNode -> objectNode.get(AttributeNames.RFC7643.USER_NAME)
                                                                       .textValue())
                                          .collect(Collectors.toList())
                                          .toString());
      Assertions.assertEquals(expectedResults, listResponse.getItemsPerPage());
      List<ScimObjectNode> resources = listResponse.getListedResources();
      Assertions.assertEquals(expectedResults, resources.size());

      if (expectedResults != 0)
      {
        Assertions.assertTrue(Arrays.stream(expectedUsers)
                                    .map(u -> u.getUserName().orElse(null))
                                    .allMatch(username -> resources.stream().anyMatch(r -> {
                                      return username.equals(r.get(AttributeNames.RFC7643.USER_NAME).textValue());
                                    })),
                              String.format("Could not find expected-users '%s' in result-list '%s'",
                                            Arrays.stream(expectedUsers)
                                                  .map(u -> u.getUserName().orElse(null))
                                                  .collect(Collectors.toList()),
                                            resources.stream()
                                                     .map(u -> u.get(AttributeNames.RFC7643.USER_NAME).textValue())
                                                     .collect(Collectors.toList())));

        Arrays.stream(expectedUsers).forEach(expectedUser -> {
          CustomUser returnedUser = resources.stream().filter(user -> {
            return user.get(AttributeNames.RFC7643.USER_NAME).textValue().equals(expectedUser.getUserName().get());
          }).map(user -> JsonHelper.copyResourceToObject(user, CustomUser.class)).findAny().get();
          UserComparator.checkUserEquality(expectedUser, returnedUser);
        });

      }
    });
  }
}
