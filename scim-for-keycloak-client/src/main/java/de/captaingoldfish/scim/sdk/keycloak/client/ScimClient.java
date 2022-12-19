package de.captaingoldfish.scim.sdk.keycloak.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.ScimRequestBuilder;
import de.captaingoldfish.scim.sdk.client.builder.BulkBuilder;
import de.captaingoldfish.scim.sdk.client.http.HttpResponse;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.Comparator;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Address;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PhoneNumber;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 05.02.2020 <br>
 * <br>
 */
@Slf4j
public class ScimClient
{

  private static final String USER_ENDPOINT = EndpointPaths.USERS;

  private static int START_INDEX = 0;

  private static int NUMBER_OF_USERS = 50000;

  /**
   * creates almost 5000 users and 5 groups with 10 random users as members for these groups
   */
  public static void main(String[] args)
  {
    final String baseUrl = "http://localhost:8080/auth/realms/scim/scim/v2";
    final String accessToken = getAccessToken();
    final Map<String, String> defaultHeaders = new HashMap<>();
    defaultHeaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    ScimRequestBuilder scimRequestBuilder = new ScimRequestBuilder(baseUrl,
                                                                   ScimClientConfig.builder()
                                                                                   .socketTimeout(120)
                                                                                   .requestTimeout(120)
                                                                                   .httpHeaders(defaultHeaders)
                                                                                   .enableAutomaticBulkRequestSplitting(true)
                                                                                   .build());

    deleteAllUsers(scimRequestBuilder);
    deleteAllGroups(scimRequestBuilder);
    createUsers(scimRequestBuilder);
    createGroups(scimRequestBuilder);
    updateGroups(scimRequestBuilder);
  }

  @SneakyThrows
  private static String getAccessToken()
  {
    final String tokenEndpoint = "http://localhost:8080/auth/realms/scim/protocol/openid-connect/token";
    ScimClientConfig scimClientConfig = ScimClientConfig.builder()
                                                        .socketTimeout(120)
                                                        .requestTimeout(120)
                                                        .basic("scim-client", "k9xE62pv4BdyY2PghQpe8oP1hKdOJU5x")
                                                        .enableAutomaticBulkRequestSplitting(true)
                                                        .build();
    try (ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig))
    {

      HttpPost httpPost = new HttpPost(tokenEndpoint);
      httpPost.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
      // httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Basic scim:5810f85b-cedd-4cc3-84cc-ccbd89d4a54a=");
      URIBuilder uriBuilder = new URIBuilder("http://localhost").addParameter("grant_type", "client_credentials")
      /*
       * .addParameter("client_id", "scim") .addParameter("client_secret", "ffc6e64a-36d2-4ea7-9140-538c4a487459")
       */;
      httpPost.setEntity(new StringEntity(uriBuilder.build().getQuery()));
      HttpResponse httpResponse = scimHttpClient.sendRequest(httpPost);
      if (httpResponse.getHttpStatusCode() != 200)
      {
        throw new IllegalStateException("retrieving access token failed");
      }
      ObjectNode objectNode = (ObjectNode)JsonHelper.readJsonDocument(httpResponse.getResponseBody());
      return objectNode.get("access_token").asText();
    }
  }

  /**
   * create almost 5000 users on keycloak
   */
  private static void createUsers(ScimRequestBuilder scimRequestBuilder)
  {
    List<User> bulkList = getUserList();
    BulkBuilder bulkBuilder = scimRequestBuilder.bulk();
    bulkList.parallelStream().forEach(user -> {
      bulkBuilder.bulkRequestOperation(USER_ENDPOINT)
                 .bulkId(UUID.randomUUID().toString())
                 .method(HttpMethod.POST)
                 .data(user)
                 .next();
    });
    ServerResponse<BulkResponse> response = bulkBuilder.sendRequest();
    if (response.isSuccess())
    {
      log.info("bulk request succeeded with response: {}", response.getResponseBody());
    }
    else
    {
      log.error("creating of users failed: " + response.getErrorResponse().getDetail().orElse(null));
    }
  }

  /**
   * will delete all users on keycloak
   */
  private static void deleteAllUsers(ScimRequestBuilder scimRequestBuilder)
  {
    ServerResponse<ListResponse<User>> response = scimRequestBuilder.list(User.class, USER_ENDPOINT)
                                                                    .attributes(AttributeNames.RFC7643.ID,
                                                                                AttributeNames.RFC7643.USER_NAME)
                                                                    .get()
                                                                    .sendRequest();
    ListResponse<User> listResponse = response.getResource();

    while (listResponse.getTotalResults() > 0)
    {
      listResponse.getListedResources().stream().parallel().forEach(user -> {
        final String username = StringUtils.lowerCase(user.get(AttributeNames.RFC7643.USER_NAME).textValue());
        ServerResponse<User> deleteResponse = scimRequestBuilder.delete(User.class,
                                                                        USER_ENDPOINT,
                                                                        user.get(AttributeNames.RFC7643.ID).textValue())
                                                                .sendRequest();
        if (deleteResponse.isSuccess())
        {
          log.trace("user with name {} was successfully deleted", username);
        }
        else
        {
          log.error("user with name {} could not be deleted", username);
        }
      });
      response = scimRequestBuilder.list(User.class, USER_ENDPOINT)
                                   .filter("username", Comparator.NE, "admin")
                                   .build()
                                   .get()
                                   .sendRequest();
      listResponse = response.getResource();
    }
  }

  /**
   * reads a lot of users and returns them as SCIM user instances
   */
  private static List<User> getUserList()
  {
    List<User> userList = new ArrayList<>();
    try (InputStream inputStream = ScimClient.class.getResourceAsStream("/firstnames.txt");
      InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
      BufferedReader reader = new BufferedReader(inputStreamReader))
    {
      int counter = 0;
      Random random = new Random();
      String name;
      while ((name = reader.readLine()) != null)
      {
        name = name.toLowerCase();
        Meta meta = Meta.builder().created(LocalDateTime.now()).lastModified(LocalDateTime.now()).build();
        User user = User.builder()
                        .userName(name)
                        .name(Name.builder()
                                  .givenName(name)
                                  .middlename(UUID.randomUUID().toString())
                                  .familyName("Mustermann")
                                  .honorificPrefix(random.nextInt(20) == 0 ? "Mr." : "Ms.")
                                  .honorificSuffix(random.nextInt(20) == 0 ? "sama" : null)
                                  .formatted(name)
                                  .build())
                        .active(random.nextBoolean())
                        .nickName(name)
                        .title("Dr.")
                        .displayName(name)
                        .userType(random.nextInt(50) == 0 ? "admin" : "user")
                        .locale(random.nextBoolean() ? "de-DE" : "en-US")
                        .preferredLanguage(random.nextBoolean() ? "de" : "en")
                        .timeZone(random.nextBoolean() ? "Europe/Berlin" : "America/Los_Angeles")
                        .profileUrl("http://localhost/" + name)
                        .emails(Arrays.asList(Email.builder()
                                                   .value(name + "@test.de")
                                                   .primary(random.nextInt(20) == 0)
                                                   .build(),
                                              Email.builder().value(name + "_the_second@test.de").build()))
                        .phoneNumbers(Arrays.asList(PhoneNumber.builder()
                                                               .value(String.valueOf(random.nextLong()
                                                                                     + Integer.MAX_VALUE))
                                                               .primary(random.nextInt(20) == 0)
                                                               .build(),
                                                    PhoneNumber.builder()
                                                               .value(String.valueOf(random.nextLong()
                                                                                     + Integer.MAX_VALUE))
                                                               .build()))
                        .addresses(Arrays.asList(Address.builder()
                                                        .streetAddress(name + " street " + random.nextInt(500))
                                                        .country(random.nextBoolean() ? "germany" : "united states")
                                                        .postalCode(String.valueOf(random.nextLong()
                                                                                   + Integer.MAX_VALUE))
                                                        .primary(random.nextInt(20) == 0)
                                                        .build(),
                                                 Address.builder()
                                                        .streetAddress(name + " second street " + random.nextInt(500))
                                                        .country(random.nextBoolean() ? "germany" : "united states")
                                                        .postalCode(String.valueOf(random.nextLong()
                                                                                   + Integer.MAX_VALUE))
                                                        .build()))
                        .meta(meta)
                        .build();
        log.info(user.toString());
        userList.add(user);
        if (++counter == NUMBER_OF_USERS)
        {
          break;
        }
      }
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
    return userList.subList(START_INDEX, userList.size());
  }

  /**
   * creates some groups with 10 random members for each group
   */
  private static void createGroups(ScimRequestBuilder scimRequestBuilder)
  {
    List<Group> groups = getGroupsList(scimRequestBuilder);
    groups.stream().parallel().forEach(group -> {
      List<Member> randomUsers = getRandomUsers(scimRequestBuilder).stream()
                                                                   .map(user -> Member.builder()
                                                                                      .type(ResourceTypeNames.USER)
                                                                                      .value(user.getId().get())
                                                                                      .build())
                                                                   .collect(Collectors.toList());
      group.setMembers(randomUsers);
      ServerResponse<Group> response = scimRequestBuilder.create(Group.class, EndpointPaths.GROUPS)
                                                         .setResource(group)
                                                         .sendRequest();
      if (response.isSuccess())
      {
        log.trace("group with name {} was successfully created", group.getDisplayName().get());
      }
      else
      {
        log.error("group with name {} could not be created", group.getDisplayName().get());
      }
    });
  }

  /**
   * creates some groups with 10 random members for each group
   */
  private static void updateGroups(ScimRequestBuilder scimRequestBuilder)
  {
    List<Group> allGroups = getAllGroups(scimRequestBuilder);
    allGroups.stream().parallel().forEach(group -> {
      List<Member> randomUsers = getRandomUsers(scimRequestBuilder).stream()
                                                                   .map(user -> Member.builder()
                                                                                      .type(ResourceTypeNames.USER)
                                                                                      .value(user.getId().get())
                                                                                      .build())
                                                                   .collect(Collectors.toList());
      group.setMembers(randomUsers);
      ServerResponse<Group> response = scimRequestBuilder.update(Group.class, EndpointPaths.GROUPS, group.getId().get())
                                                         .setResource(group)
                                                         .sendRequest();
      if (response.isSuccess())
      {
        log.trace("group with name {} was successfully created", group.getDisplayName().get());
      }
      else
      {
        log.error("group with name {} could not be created", group.getDisplayName().get());
      }
    });
  }

  /**
   * reads a lot of users and returns them as SCIM user instances
   */
  private static List<Group> getGroupsList(ScimRequestBuilder scimRequestBuilder)
  {
    List<Group> groupList = new ArrayList<>();
    try (InputStream inputStream = ScimClient.class.getResourceAsStream("/groupnames.txt");
      InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
      BufferedReader reader = new BufferedReader(inputStreamReader))
    {

      String name;
      while ((name = reader.readLine()) != null)
      {
        Meta meta = Meta.builder().created(LocalDateTime.now()).lastModified(LocalDateTime.now()).build();
        List<User> randomUsers = getRandomUsers(scimRequestBuilder);
        List<Member> userMember = randomUsers.stream()
                                             .map(user -> Member.builder()
                                                                .type(ResourceTypeNames.USER)
                                                                .value(user.getId().get())
                                                                .build())
                                             .collect(Collectors.toList());
        groupList.add(Group.builder().displayName(name).members(userMember).meta(meta).build());
      }
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
    return groupList;
  }

  /**
   * deletes all groups from the current realm
   */
  private static void deleteAllGroups(ScimRequestBuilder scimRequestBuilder)
  {
    List<Group> allGroups = getAllGroups(scimRequestBuilder);
    allGroups.parallelStream().forEach(group -> {
      ServerResponse<Group> deleteResponse = scimRequestBuilder.delete(Group.class,
                                                                       EndpointPaths.GROUPS,
                                                                       group.getId().get())
                                                               .sendRequest();

      if (deleteResponse.isSuccess())
      {
        log.trace("group with name {} was successfully deleted", group.getDisplayName().get());
      }
      else
      {
        log.error("group with name {} could not be deleted", group.getDisplayName().get());
      }
    });
  }

  private static List<Group> getAllGroups(ScimRequestBuilder scimRequestBuilder)
  {
    return scimRequestBuilder.list(Group.class, EndpointPaths.GROUPS)
                             .get()
                             .sendRequest()
                             .getResource()
                             .getListedResources();
  }

  /**
   * randomly gets 10 users from the server
   */
  private static List<User> getRandomUsers(ScimRequestBuilder scimRequestBuilder)
  {
    Random random = new Random();
    ServerResponse<ListResponse<User>> countResponse = scimRequestBuilder.list(User.class, USER_ENDPOINT)
                                                                         .count(0)
                                                                         .get()
                                                                         .sendRequest();
    long totalResults = countResponse.getResource().getTotalResults();
    ServerResponse<ListResponse<User>> response = scimRequestBuilder.list(User.class, USER_ENDPOINT)
                                                                    .startIndex(random.nextInt((int)totalResults))
                                                                    .count(150)
                                                                    .get()
                                                                    .sendRequest();
    ListResponse<User> listResponse = response.getResource();
    return listResponse.getListedResources()
                       .stream()
                       .map(user -> JsonHelper.copyResourceToObject(user, User.class))
                       .collect(Collectors.toList());
  }

}
