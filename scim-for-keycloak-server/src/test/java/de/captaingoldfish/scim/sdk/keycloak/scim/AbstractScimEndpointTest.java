package de.captaingoldfish.scim.sdk.keycloak.scim;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.databind.node.BooleanNode;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.scim.administration.ServiceProviderResource;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;
import de.captaingoldfish.scim.sdk.keycloak.setup.FileReferences;
import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;


/**
 * Automatically enables the SCIM Endpoint for the default realm.
 *
 * @author Mario Siegenthaler
 * @since 07.08.2020
 */
public class AbstractScimEndpointTest extends KeycloakScimManagementTest implements FileReferences
{

  @BeforeEach
  public void enableScim()
  {
    ServiceProvider serviceProvider = getScimEndpoint().getResourceEndpoint().getServiceProvider();
    serviceProvider.set("enabled", BooleanNode.valueOf(true));
    ServiceProviderResource serviceProviderResource = getScimEndpoint().administerResources()
                                                                       .getServiceProviderResource();
    serviceProviderResource.updateServiceProviderConfig(serviceProvider.toString());
  }

  /**
   * creates a user using the SCIM endpoint
   */
  public CustomUser createUser(User user)
  {
    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .method(HttpMethod.POST)
                                               .endpoint(EndpointPaths.USERS)
                                               .requestBody(user.toString())
                                               .build();
    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());
    return JsonHelper.readJsonDocument(response.readEntity(String.class), CustomUser.class);
  }

  /**
   * updates an existing user using the SCIM endpoint
   */
  public CustomUser updateUser(String id, User user)
  {
    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .method(HttpMethod.PUT)
                                               .endpoint(EndpointPaths.USERS + "/" + id)
                                               .requestBody(user.toString())
                                               .build();
    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.OK, response.getStatus());
    return JsonHelper.readJsonDocument(response.readEntity(String.class), CustomUser.class);
  }

  /**
   * deletes a user using the SCIM endpoint
   */
  public void deleteUser(User user)
  {
    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .method(HttpMethod.DELETE)
                                               .endpoint(String.format("%s/%s",
                                                                       EndpointPaths.USERS,
                                                                       user.getId().get()))
                                               .build();
    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatus());
  }
}
