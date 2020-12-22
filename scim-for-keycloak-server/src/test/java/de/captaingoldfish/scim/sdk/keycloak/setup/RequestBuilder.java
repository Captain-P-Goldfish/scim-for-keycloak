package de.captaingoldfish.scim.sdk.keycloak.setup;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimEndpoint;


/**
 * @author Pascal Knueppel
 * @since 07.08.2020
 */
public class RequestBuilder
{

  public static final String DEFAULT_REQUEST_URL = "http://localhost:8080/auth/realms/master/scim/v2";

  private HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

  private ScimEndpoint scimEndpoint;

  public RequestBuilder(ScimEndpoint scimEndpoint)
  {
    this.scimEndpoint = scimEndpoint;
    Map<String, String> defaultHeaders = new HashMap<>();
    defaultHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
    headers(defaultHeaders);
    method(HttpMethod.GET);
    requestBody(null);
  }

  public static RequestBuilder builder(ScimEndpoint scimEndpoint)
  {
    return new RequestBuilder(scimEndpoint);
  }

  public RequestBuilder requestBody(String requestBody)
  {
    Mockito.doReturn(requestBody).when(scimEndpoint).getRequestBody(Mockito.any());
    return this;
  }

  public RequestBuilder queryString(String queryString)
  {
    Mockito.doReturn(queryString).when(request).getQueryString();
    return this;
  }

  public RequestBuilder endpoint(String endpoint)
  {
    Mockito.doReturn(new StringBuffer(DEFAULT_REQUEST_URL + endpoint)).when(request).getRequestURL();
    return this;
  }

  public RequestBuilder method(HttpMethod method)
  {
    Mockito.doReturn(method.toString()).when(request).getMethod();
    return this;
  }

  public RequestBuilder headers(Map<String, String> httpHeaders)
  {
    Mockito.doReturn(httpHeaders).when(scimEndpoint).getHttpHeaders(Mockito.any());
    return this;
  }

  public HttpServletRequest build()
  {
    return request;
  }


}
