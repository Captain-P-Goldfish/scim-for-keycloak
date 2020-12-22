package de.captaingoldfish.scim.sdk.keycloak.scim.administration;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.storage.ClientStorageManager;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractEndpoint;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimConfiguration;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimServiceProviderService;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;


/**
 * @author Pascal Knueppel
 * @since 08.08.2020
 */
public class ServiceProviderResource extends AbstractEndpoint
{

  public ServiceProviderResource(KeycloakSession keycloakSession)
  {
    super(keycloakSession);
  }

  /**
   * retrieves the current service provider configuration. This must be done over the administration resource
   * because the configuration could not be read anymore if the SCIM service would be disabled
   *
   * @return the current service provider configuration with an additional enabled attribute
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getServiceProviderConfig()
  {
    ScimServiceProviderService scimServiceProviderService = new ScimServiceProviderService(getKeycloakSession());
    ServiceProvider currentServiceProvider = scimServiceProviderService.getServiceProvider();
    return Response.ok().entity(currentServiceProvider.toString()).build();
  }

  /**
   * updates the current service provider configuration
   *
   * @param content the request body from the admin-console
   * @return changes the service provider configuration of the current realm
   */
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateServiceProviderConfig(final String content)
  {
    ServiceProvider newServiceProvider = JsonHelper.readJsonDocument(content, ServiceProvider.class);
    ScimServiceProviderService scimServiceProviderService = new ScimServiceProviderService(getKeycloakSession());
    newServiceProvider = scimServiceProviderService.updateServiceProvider(newServiceProvider);

    // now override the current service provider configuration
    {
      ResourceEndpoint resourceEndpoint = ScimConfiguration.getScimEndpoint(getKeycloakSession(), true);
      ServiceProvider oldServiceProvider = resourceEndpoint.getServiceProvider();
      oldServiceProvider.setFilterConfig(newServiceProvider.getFilterConfig());
      oldServiceProvider.setSortConfig(newServiceProvider.getSortConfig());
      oldServiceProvider.setPatchConfig(newServiceProvider.getPatchConfig());
      oldServiceProvider.setETagConfig(newServiceProvider.getETagConfig());
      oldServiceProvider.setChangePasswordConfig(newServiceProvider.getChangePasswordConfig());
      oldServiceProvider.setBulkConfig(newServiceProvider.getBulkConfig());
      final Instant lastModified = newServiceProvider.getMeta().flatMap(Meta::getLastModified).orElse(null);
      final ETag version = newServiceProvider.getMeta().flatMap(Meta::getVersion).orElse(null);
      Optional<Meta> metaOptional = oldServiceProvider.getMeta();
      metaOptional.ifPresent(meta -> {
        // lastModified will never be null
        meta.setLastModified(lastModified);
        meta.setVersion(version);
      });
    }

    return Response.ok().entity(newServiceProvider.toString()).build();
  }

  /**
   * @return the clients that may be used to restrict access to the SCIM endpoint
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/availableClients")
  public Response getAvailableClients()
  {
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    ClientStorageManager clientProvider = new ClientStorageManager(getKeycloakSession(), 10000);

    ScimServiceProviderService scimServiceProviderService = new ScimServiceProviderService(getKeycloakSession());
    ScimServiceProviderEntity serviceProvider = scimServiceProviderService.getServiceProviderEntity()
                                                                          .orElseThrow(IllegalStateException::new);

    List<String> assignedClientIds = serviceProvider.getAuthorizedClients()
                                                    .stream()
                                                    .map(ClientEntity::getClientId)
                                                    .collect(Collectors.toList());
    Stream<String> clientModelStream = clientProvider.getClientsStream(getKeycloakSession().getContext().getRealm())
                                                     .map(ClientModel::getClientId)
                                                     .filter(clientId -> !assignedClientIds.contains(clientId));
    clientModelStream.forEach(arrayNode::add);
    return Response.ok(arrayNode.toString()).build();
  }

  /**
   * overridden to grant access to unit test scope
   */
  @Override
  protected ResourceEndpoint getResourceEndpoint()
  {
    return super.getResourceEndpoint();
  }
}
