package de.captaingoldfish.scim.sdk.keycloak.scim;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.endpoints.CustomUserEndpoint;
import de.captaingoldfish.scim.sdk.keycloak.scim.endpoints.RoleEndpointDefinition;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.GroupHandler;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.RealmRoleHandler;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.UserHandler;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimResourceTypeService;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimServiceProviderService;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.base.GroupEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 04.02.2020
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScimConfiguration
{

  /**
   * holds a different SCIM configuration and endpoints for each realm <br />
   * <br />
   * the key of the map is the name of the realm
   */
  @Getter(AccessLevel.PROTECTED) // used for unit tests
  private static final Map<String, ResourceEndpoint> RESOURCE_ENDPOINT_MAP = new HashMap<>();

  public static void realmRemoved(KeycloakSession keycloakSession)
  {
    RealmModel realmModel = keycloakSession.getContext().getRealm();
    RESOURCE_ENDPOINT_MAP.remove(realmModel.getId());
  }

  /**
   * gets the SCIM resource endpoint for the given realm
   *
   * @param keycloakSession used to check for existing {@link ServiceProvider}s in the database
   * @param createEndpointIfNotExisting if set to true the {@link ResourceEndpoint} will be created if not
   *          already present
   * @return the SCIM resource endpoint for the given realm
   */
  public static ResourceEndpoint getScimEndpoint(KeycloakSession keycloakSession, boolean createEndpointIfNotExisting)
  {
    RealmModel realm = keycloakSession.getContext().getRealm();
    ResourceEndpoint resourceEndpoint = RESOURCE_ENDPOINT_MAP.get(realm.getName());
    if (resourceEndpoint == null && createEndpointIfNotExisting)
    {
      resourceEndpoint = createNewResourceEndpoint(keycloakSession);
      RESOURCE_ENDPOINT_MAP.put(realm.getName(), resourceEndpoint);
    }
    return resourceEndpoint;
  }

  /**
   * creates a new resource endpoint for the current realm
   */
  private static ResourceEndpoint createNewResourceEndpoint(KeycloakSession keycloakSession)
  {
    ScimServiceProviderService scimServiceProviderService = new ScimServiceProviderService(keycloakSession);
    ServiceProvider serviceProvider = scimServiceProviderService.getServiceProvider();
    ResourceEndpoint resourceEndpoint = new ResourceEndpoint(serviceProvider);

    ScimResourceTypeService resourceTypeService = new ScimResourceTypeService(keycloakSession);

    ResourceType userResourceType = resourceEndpoint.registerEndpoint(new CustomUserEndpoint(new UserHandler()));
    userResourceType.setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).autoSorting(true).build());
    ScimResourceTypeEntity userResourceTypeEntity = resourceTypeService.getOrCreateResourceTypeEntry(userResourceType);
    resourceTypeService.updateResourceType(userResourceType, userResourceTypeEntity);

    ResourceType groupResourceType = resourceEndpoint.registerEndpoint(new GroupEndpointDefinition(new GroupHandler()));
    groupResourceType.setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).autoSorting(true).build());
    ScimResourceTypeEntity groupResourceTypeEntity = resourceTypeService.getOrCreateResourceTypeEntry(groupResourceType);
    resourceTypeService.updateResourceType(groupResourceType, groupResourceTypeEntity);

    ResourceType roleResourceType = resourceEndpoint.registerEndpoint(new RoleEndpointDefinition(new RealmRoleHandler()));
    roleResourceType.setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).autoSorting(true).build());
    ScimResourceTypeEntity roleResourceTypeEntity = resourceTypeService.getOrCreateResourceTypeEntry(roleResourceType);
    resourceTypeService.updateResourceType(roleResourceType, roleResourceTypeEntity);

    return resourceEndpoint;
  }

}
