package de.captaingoldfish.scim.sdk.keycloak.setup;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimEndpoint;
import lombok.SneakyThrows;


/**
 * @author Pascal Knueppel
 * @since 07.08.2020
 */
public class RequestMock
{

  public static final String DEFAULT_REQUEST_URL = "http://localhost:8080/auth/realms/master/scim/v2";

  private HttpRequest request = Mockito.mock(HttpRequest.class);

  private KeycloakUriInfo uriInfo = Mockito.mock(KeycloakUriInfo.class);

  private ScimEndpoint scimEndpoint;

  @SneakyThrows
  public RequestMock(ScimEndpoint scimEndpoint, KeycloakSession keycloakSession)
  {
    this.scimEndpoint = scimEndpoint;
    KeycloakContext keycloakContext = keycloakSession.getContext();
    Mockito.doReturn(request).when(keycloakContext).getContextObject(Mockito.eq(HttpRequest.class));
    Mockito.doReturn(uriInfo).when(keycloakContext).getUri();
    Mockito.doReturn(new MultivaluedHashMap<>()).when(uriInfo).getQueryParameters();
    Map<String, String> defaultHeaders = new HashMap<>();
    defaultHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
    headers(defaultHeaders);
    method(HttpMethod.GET);
  }

  public static RequestMock mockRequest(ScimEndpoint scimEndpoint, KeycloakSession keycloakSession)
  {
    return new RequestMock(scimEndpoint, keycloakSession);
  }

  @SneakyThrows
  public RequestMock endpoint(String endpoint)
  {
    final String url = DEFAULT_REQUEST_URL + endpoint;
    Mockito.doReturn(new URI(url)).when(uriInfo).getAbsolutePath();
    return this;
  }

  public RequestMock method(HttpMethod method)
  {
    Mockito.doReturn(method.toString()).when(request).getHttpMethod();
    return this;
  }

  public RequestMock headers(Map<String, String> httpHeaders)
  {
    Mockito.doReturn(httpHeaders).when(scimEndpoint).getHttpHeaders(Mockito.any());
    return this;
  }

}
