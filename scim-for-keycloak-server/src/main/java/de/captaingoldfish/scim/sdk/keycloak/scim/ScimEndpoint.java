package de.captaingoldfish.scim.sdk.keycloak.scim;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.models.KeycloakSession;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.keycloak.auth.Authentication;
import de.captaingoldfish.scim.sdk.keycloak.auth.ScimAuthorization;
import de.captaingoldfish.scim.sdk.keycloak.constants.ContextPaths;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.administration.AdminstrationResource;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimServiceProviderService;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 04.02.2020 <br>
 * <br>
 */
@Slf4j
public class ScimEndpoint extends AbstractEndpoint
{

  /**
   * the authentication implementation
   */
  private Authentication authentication;

  /**
   * @param authentication used as constructor param to pass a mockito mock during unit testing
   */
  public ScimEndpoint(KeycloakSession keycloakSession, Authentication authentication)
  {
    super(keycloakSession);
    this.authentication = authentication;
  }

  /**
   * provides functionality to configure the SCIM endpoints
   */
  @Path(ContextPaths.ADMIN)
  public AdminstrationResource administerResources()
  {
    return new AdminstrationResource(getKeycloakSession(), authentication);
  }

  /**
   * handles all SCIM requests
   *
   * @param request the server request object
   * @return the jax-rs response
   */
  @POST
  @GET
  @PUT
  @PATCH
  @DELETE
  @Path(ContextPaths.SCIM_ENDPOINT_PATH + "/{s:.*}")
  @Produces(HttpHeader.SCIM_CONTENT_TYPE)
  public Response handleScimRequest(@Context HttpServletRequest request)
  {
    ScimServiceProviderService scimServiceProviderService = new ScimServiceProviderService(getKeycloakSession());
    Optional<ScimServiceProviderEntity> serviceProviderEntity = scimServiceProviderService.getServiceProviderEntity();
    if (serviceProviderEntity.isPresent() && !serviceProviderEntity.get().isEnabled())
    {
      throw new NotFoundException();
    }
    ResourceEndpoint resourceEndpoint = getResourceEndpoint();

    ScimAuthorization scimAuthorization = new ScimAuthorization(getKeycloakSession(), authentication);
    ScimKeycloakContext scimKeycloakContext = new ScimKeycloakContext(getKeycloakSession(), scimAuthorization);

    String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
    ScimResponse scimResponse = resourceEndpoint.handleRequest(request.getRequestURL().toString() + query,
                                                               HttpMethod.valueOf(request.getMethod()),
                                                               getRequestBody(request),
                                                               getHttpHeaders(request),
                                                               null,
                                                               commitOrRollback(),
                                                               scimKeycloakContext);
    return scimResponse.buildResponse();
  }

  /**
   * commit or rollback the transaction
   */
  private BiConsumer<ScimResponse, Boolean> commitOrRollback()
  {
    return (scimResponse, isError) -> {
      try
      {
        if (isError)
        {
          // if the request has failed roll the transaction back
          getKeycloakSession().getTransactionManager().setRollbackOnly();
        }
        else
        {
          // if the request succeeded commit the transaction
          getKeycloakSession().getTransactionManager().commit();
        }
      }
      catch (Exception ex)
      {
        throw new InternalServerException(ex.getMessage());
      }
    };
  }

  /**
   * reads the request body from the input stream of the request object
   *
   * @param request the request object
   * @return the request body as string
   */
  public String getRequestBody(HttpServletRequest request)
  {
    try (InputStream inputStream = request.getInputStream())
    {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  /**
   * extracts the http headers from the request and puts them into a map
   *
   * @param request the request object
   * @return a map with the http-headers
   */
  public Map<String, String> getHttpHeaders(HttpServletRequest request)
  {
    Map<String, String> httpHeaders = new HashMap<>();
    Enumeration<String> enumeration = request.getHeaderNames();
    while (enumeration != null && enumeration.hasMoreElements())
    {
      String headerName = enumeration.nextElement();
      String headerValue = request.getHeader(headerName);

      boolean isContentTypeHeader = HttpHeader.CONTENT_TYPE_HEADER.toLowerCase(Locale.ROOT)
                                                                  .equals(headerName.toLowerCase(Locale.ROOT));
      boolean isApplicationJson = StringUtils.startsWithIgnoreCase(headerValue, "application/json");
      if (isContentTypeHeader && isApplicationJson)
      {
        headerValue = HttpHeader.SCIM_CONTENT_TYPE;
      }
      httpHeaders.put(headerName, headerValue);
    }
    return httpHeaders;
  }

}
