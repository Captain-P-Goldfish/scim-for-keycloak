package de.captaingoldfish.scim.sdk.keycloak.services;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractScimEndpointTest;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimConfigurationBridge;
import de.captaingoldfish.scim.sdk.keycloak.scim.endpoints.CustomUserLegacyEndpoint;
import de.captaingoldfish.scim.sdk.keycloak.scim.helper.UserComparator;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;


/**
 * @author Pascal Knueppel
 * @since 22.12.2022
 */
public class UserLegacyDataMigrationServiceTest extends AbstractScimEndpointTest
{

  @BeforeEach
  public void activateLegacyEndpoint()
  {
    ResourceEndpoint resourceEndpoint = ScimConfigurationBridge.getScimResourceEndpoints()
                                                               .get(getRealmModel().getName());
    ResourceType resourceType = resourceEndpoint.getResourceTypeByName("UserLegacy").get();
    resourceType.setDisabled(false);
  }


  @Test
  public void testUserLegacyDataMigrationServiceTest()
  {
    CustomUser superMarioScim = JsonHelper.loadJsonDocument(USER_SUPER_MARIO, CustomUser.class);
    CustomUser donkeyKongScim = JsonHelper.loadJsonDocument(USER_DONKEY_KONG, CustomUser.class);
    CustomUser linkScim = JsonHelper.loadJsonDocument(USER_LINK, CustomUser.class);

    superMarioScim = createLegacyUser(superMarioScim);
    donkeyKongScim = createLegacyUser(donkeyKongScim);
    linkScim = createLegacyUser(linkScim);

    UserLegacyDataMigrationService migrationService = new UserLegacyDataMigrationService(getKeycloakSession());
    migrationService.migrateUserData();

    for ( CustomUser user : Arrays.asList(superMarioScim, donkeyKongScim, linkScim) )
    {
      CustomUser returnedUser = getUser(user.getId().get());
      UserComparator.checkUserEquality(user, returnedUser);
      checkUserAttributesAreEmpty(user);
    }
  }

  /**
   * makes sure that the attributes from the user-attributes table were deleted after migration
   */
  private void checkUserAttributesAreEmpty(CustomUser user)
  {
    UserModel userModel = getKeycloakSession().users().getUserById(getRealmModel(), user.getId().get());
    Map<String, List<String>> attributesMap = userModel.getAttributes();
    attributesMap.remove(UserModel.USERNAME);
    attributesMap.remove(UserModel.FIRST_NAME);
    attributesMap.remove(UserModel.LAST_NAME);
    attributesMap.remove(UserModel.EMAIL);

    Assertions.assertTrue(attributesMap.isEmpty(), attributesMap.values().toString());
  }

  /**
   * creates a user using the SCIM endpoint
   */
  private CustomUser createLegacyUser(User user)
  {
    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .method(HttpMethod.POST)
                                               .endpoint(CustomUserLegacyEndpoint.USER_LEGACY_PATH)
                                               .requestBody(user.toString())
                                               .build();
    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());
    return JsonHelper.readJsonDocument(response.readEntity(String.class), CustomUser.class);
  }
}
