package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.ChangePasswordConfig;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimConfigurationBridge;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractScimEndpointTest;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
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

    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .method(HttpMethod.PATCH)
                                               .endpoint(EndpointPaths.USERS + "/" + superMario.getId())
                                               .requestBody(patchOpRequest.toString())
                                               .build();
    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.OK, response.getStatus());

    // validate
    {
      UserCredentialModel newUserCredential = UserCredentialModel.password(newPassword);
      Assertions.assertFalse(credentialManager.isValid(getRealmModel(), superMario, originalUserCredential));
      Assertions.assertTrue(credentialManager.isValid(getRealmModel(), superMario, newUserCredential));
    }
  }
}
