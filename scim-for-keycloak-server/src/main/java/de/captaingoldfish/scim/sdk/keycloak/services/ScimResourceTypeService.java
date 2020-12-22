package de.captaingoldfish.scim.sdk.keycloak.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.RoleEntity;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimConfiguration;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.ParseableResourceType;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ETagFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.EndpointControlFeature;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeAuthorization;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 05.08.2020
 */
@Slf4j
public class ScimResourceTypeService extends AbstractService
{


  public ScimResourceTypeService(KeycloakSession keycloakSession)
  {
    super(keycloakSession);
  }

  /**
   * gets an existing configuration from the database for the given resource type or creates a configuration in
   * the database that matches the settings of the given resource type if no entry for this resource type was
   * present yet
   * 
   * @param resourceType the resource type that might have a configuration in the database or not
   * @return the existing configuration or a configuration that matches the given resource type
   */
  public ScimResourceTypeEntity getOrCreateResourceTypeEntry(ResourceType resourceType)
  {
    RealmModel realmModel = getKeycloakSession().getContext().getRealm();
    Optional<ScimResourceTypeEntity> optionalResourceTypeEntity = getResourceTypeEntityByName(resourceType.getName());
    if (!optionalResourceTypeEntity.isPresent())
    {
      log.info("no database entry found for resource type {}. Entry will be created", resourceType.getName());
      return createNewResourceTypeEntry(resourceType, realmModel);
    }
    return optionalResourceTypeEntity.get();
  }

  /**
   * uses the given parsed resource type data and puts its values into the corresponding database entry
   * 
   * @param parseableResourceType the resource type data that was parsed from a http request body
   * @return empty if no entry with the given resource type name exists in the database and the updated database
   *         entry if the update was successful
   */
  public Optional<ScimResourceTypeEntity> updateDatabaseEntry(ParseableResourceType parseableResourceType)
  {
    Optional<ScimResourceTypeEntity> resourceTypeEntity = getResourceTypeEntityByName(parseableResourceType.getName());
    if (!resourceTypeEntity.isPresent())
    {
      return resourceTypeEntity;
    }
    ScimResourceTypeEntity scimResourceTypeEntity = resourceTypeEntity.get();
    setValuesOfEntity(scimResourceTypeEntity, parseableResourceType);
    scimResourceTypeEntity.setLastModified(Instant.now());
    getEntityManager().flush();
    return Optional.of(scimResourceTypeEntity);
  }

  /**
   * creates a new database entry for the given resource type
   * 
   * @param resourceType the resource type whose representation should be found within the database
   * @param realmModel the owning realm of the resource type
   * @return the database representation of the resource type
   */
  private ScimResourceTypeEntity createNewResourceTypeEntry(ResourceType resourceType, RealmModel realmModel)
  {
    ScimResourceTypeEntity scimResourceTypeEntity = ScimResourceTypeEntity.builder()
                                                                          .realmId(realmModel.getId())
                                                                          .name(resourceType.getName())
                                                                          .created(Instant.now())
                                                                          .build();
    getEntityManager().persist(scimResourceTypeEntity);
    setValuesOfEntity(scimResourceTypeEntity, resourceType);
    getEntityManager().flush();
    return scimResourceTypeEntity;
  }

  /**
   * tries to find a resource type within the database by its name
   * 
   * @param name the resource type name that may have a database entry
   * @return the database representation of the resource type or an empty
   */
  public Optional<ScimResourceTypeEntity> getResourceTypeEntityByName(String name)
  {
    RealmModel realmModel = getKeycloakSession().getContext().getRealm();
    try
    {
      return Optional.of(getEntityManager().createNamedQuery("getScimResourceType", ScimResourceTypeEntity.class)
                                           .setParameter("realmId", realmModel.getId())
                                           .setParameter("name", name)
                                           .getSingleResult());
    }
    catch (NoResultException ex)
    {
      return Optional.empty();
    }
  }

  /**
   * adds the values of the database entity into the scim representation of the resource type
   */
  public void updateResourceType(ResourceType resourceType, ScimResourceTypeEntity scimResourceTypeEntity)
  {
    resourceType.setDescription(scimResourceTypeEntity.getDescription());

    ResourceTypeFeatures features = resourceType.getFeatures();
    features.setResourceTypeDisabled(!scimResourceTypeEntity.isEnabled());
    features.setAutoFiltering(scimResourceTypeEntity.isAutoFiltering());
    features.setAutoSorting(scimResourceTypeEntity.isAutoSorting());
    features.setETagFeature(ETagFeature.builder().enabled(scimResourceTypeEntity.isEtagEnabled()).build());

    EndpointControlFeature endpointControl = features.getEndpointControlFeature();
    endpointControl.setCreateDisabled(scimResourceTypeEntity.isDisableCreate());
    endpointControl.setGetDisabled(scimResourceTypeEntity.isDisableGet());
    endpointControl.setListDisabled(scimResourceTypeEntity.isDisableList());
    endpointControl.setUpdateDisabled(scimResourceTypeEntity.isDisableUpdate());
    endpointControl.setDeleteDisabled(scimResourceTypeEntity.isDisableDelete());

    ResourceTypeAuthorization authorization = features.getAuthorization();
    authorization.setAuthenticated(scimResourceTypeEntity.isRequireAuthentication());
    Function<List<RoleEntity>, Set<String>> translateRoles = roleEntityList -> {
      return roleEntityList.stream().map(RoleEntity::getName).collect(Collectors.toSet());
    };
    authorization.setRoles(translateRoles.apply(scimResourceTypeEntity.getEndpointRoles()));
    authorization.setRolesCreate(translateRoles.apply(scimResourceTypeEntity.getCreateRoles()));
    authorization.setRolesGet(translateRoles.apply(scimResourceTypeEntity.getGetRoles()));
    authorization.setRolesUpdate(translateRoles.apply(scimResourceTypeEntity.getUpdateRoles()));
    authorization.setRolesDelete(translateRoles.apply(scimResourceTypeEntity.getDeleteRoles()));

    resourceType.getMeta().ifPresent(meta -> {
      meta.setCreated(scimResourceTypeEntity.getCreated());
      meta.setLastModified(scimResourceTypeEntity.getLastModified());
    });
  }

  /**
   * adds the values of the given scim resource type into the database entity
   */
  private void setValuesOfEntity(ScimResourceTypeEntity scimResourceTypeEntity, ResourceType resourceType)
  {
    scimResourceTypeEntity.setDescription(resourceType.getDescription().orElse(null));

    ResourceTypeFeatures features = resourceType.getFeatures();
    scimResourceTypeEntity.setEnabled(!features.isResourceTypeDisabled());
    scimResourceTypeEntity.setAutoFiltering(features.isAutoFiltering());
    scimResourceTypeEntity.setAutoSorting(features.isAutoSorting());
    scimResourceTypeEntity.setEtagEnabled(features.getETagFeature().isEnabled());

    scimResourceTypeEntity.setDisableCreate(features.getEndpointControlFeature().isCreateDisabled());
    scimResourceTypeEntity.setDisableGet(features.getEndpointControlFeature().isGetDisabled());
    scimResourceTypeEntity.setDisableList(features.getEndpointControlFeature().isListDisabled());
    scimResourceTypeEntity.setDisableUpdate(features.getEndpointControlFeature().isUpdateDisabled());
    scimResourceTypeEntity.setDisableDelete(features.getEndpointControlFeature().isDeleteDisabled());

    scimResourceTypeEntity.setRequireAuthentication(features.getAuthorization().isAuthenticated());
    scimResourceTypeEntity.setLastModified(resourceType.getMeta().flatMap(Meta::getLastModified).orElse(Instant.now()));

    ResourceTypeAuthorization authorization = features.getAuthorization();
    scimResourceTypeEntity.setEndpointRoles(getRoles(authorization.getRoles()));
    scimResourceTypeEntity.setCreateRoles(getRoles(authorization.getRolesCreate()));
    scimResourceTypeEntity.setGetRoles(getRoles(authorization.getRolesGet()));
    scimResourceTypeEntity.setUpdateRoles(getRoles(authorization.getRolesUpdate()));
    scimResourceTypeEntity.setDeleteRoles(getRoles(authorization.getRolesDelete()));
  }

  /**
   * retrieves the realm roles of the given names from the database. If a role does not exist within the
   * database it is simply ignored and removed
   * 
   * @param roles the roles that should be added to the current resource type
   * @return the list of role entities that do exist in the keycloak_roles table
   */
  private List<RoleEntity> getRoles(Set<String> roles)
  {
    RealmModel realmModel = getKeycloakSession().getContext().getRealm();
    List<RoleEntity> roleEntityList = new ArrayList<>();
    for ( String roleName : roles )
    {
      loadRole(realmModel, roleName).ifPresent(roleEntityList::add);
    }
    return roleEntityList;
  }

  private Optional<RoleEntity> loadRole(RealmModel realmModel, String roleName)
  {
    EntityManager entityManager = getEntityManager();
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<RoleEntity> roleQuery = criteriaBuilder.createQuery(RoleEntity.class);
    Root<RoleEntity> root = roleQuery.from(RoleEntity.class);
    // @formatter:off
    roleQuery.where(
      criteriaBuilder.and(
        criteriaBuilder.equal(root.get("realm").get("id"), realmModel.getId()),
        criteriaBuilder.equal(root.get("name"), roleName)
      )
    );
    // @formatter:on
    try
    {
      return Optional.of(entityManager.createQuery(roleQuery).getSingleResult());
    }
    catch (NoResultException ex)
    {
      return Optional.empty();
    }
  }

  /**
   * adds the values of the given scim resource type into the database entity
   */
  private void setValuesOfEntity(ScimResourceTypeEntity scimResourceTypeEntity, ParseableResourceType resourceType)
  {
    boolean enabled = Optional.ofNullable(resourceType.get("enabled")).map(JsonNode::booleanValue).orElse(true);
    scimResourceTypeEntity.setEnabled(enabled);

    scimResourceTypeEntity.setDescription(resourceType.getDescription().orElse(null));

    ResourceTypeFeatures features = resourceType.getFeatures();
    scimResourceTypeEntity.setEnabled(!features.isResourceTypeDisabled());
    scimResourceTypeEntity.setAutoFiltering(features.isAutoFiltering());
    scimResourceTypeEntity.setAutoSorting(features.isAutoSorting());
    scimResourceTypeEntity.setEtagEnabled(features.getETagFeature().isEnabled());

    scimResourceTypeEntity.setDisableCreate(features.getEndpointControlFeature().isCreateDisabled());
    scimResourceTypeEntity.setDisableGet(features.getEndpointControlFeature().isGetDisabled());
    scimResourceTypeEntity.setDisableList(features.getEndpointControlFeature().isListDisabled());
    scimResourceTypeEntity.setDisableUpdate(features.getEndpointControlFeature().isUpdateDisabled());
    scimResourceTypeEntity.setDisableDelete(features.getEndpointControlFeature().isDeleteDisabled());

    scimResourceTypeEntity.setRequireAuthentication(features.getAuthorization().isAuthenticated());
    scimResourceTypeEntity.setLastModified(Instant.now());

    ResourceTypeAuthorization authorization = features.getAuthorization();
    scimResourceTypeEntity.setEndpointRoles(getRoles(authorization.getRoles()));
    scimResourceTypeEntity.setCreateRoles(getRoles(authorization.getRolesCreate()));
    scimResourceTypeEntity.setGetRoles(getRoles(authorization.getRolesGet()));
    scimResourceTypeEntity.setUpdateRoles(getRoles(authorization.getRolesUpdate()));
    scimResourceTypeEntity.setDeleteRoles(getRoles(authorization.getRolesDelete()));
  }

  /**
   * deletes the resource types that are associated with the current realm
   */
  public void deleteResourceTypes()
  {
    RealmModel realmModel = getKeycloakSession().getContext().getRealm();
    getEntityManager().createNamedQuery("removeScimResourceTypes")
                      .setParameter("realmId", realmModel.getId())
                      .executeUpdate();
    getEntityManager().flush();
  }

  /**
   * gets all realm roles that are not present within the given list<br>
   * <br>
   * the expected SQL statement is:
   * 
   * <pre>
   *   SELECT name FROM KEYCLOAK_ROLE WHERE realmId = :realmId and is not clientRole and name not in (:roles)
   * </pre>
   * 
   * @param roles the lists that are already assigned to the current resource type
   * @return the set of roles that have not been assigned to the current resource type
   */
  public Set<String> getAvailableRolesFor(Set<String> roles)
  {
    RealmModel realmModel = getKeycloakSession().getContext().getRealm();

    CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
    CriteriaQuery<String> getAvailableRoles = criteriaBuilder.createQuery(String.class);
    Root<RoleEntity> root = getAvailableRoles.from(RoleEntity.class);

    getAvailableRoles.select(root.get("name"));

    List<Predicate> predicateList = new ArrayList<>();
    predicateList.add(criteriaBuilder.equal(root.get("realmId"), realmModel.getId()));
    predicateList.add(criteriaBuilder.not(root.get("clientRole")));
    if (roles != null && !roles.isEmpty())
    {
      predicateList.add(criteriaBuilder.not(root.get("name").in(roles)));
    }
    getAvailableRoles.where(criteriaBuilder.and(predicateList.toArray(new Predicate[0])));

    return new HashSet<>(getEntityManager().createQuery(getAvailableRoles).getResultList());
  }

  /**
   * this method will remove all associations with the given role from all resource types
   * 
   * @param roleModel the role that should be removed from the mapping tables
   */
  public void removeAssociatedRoles(RoleModel roleModel)
  {
    removeRolesFromDatabase(roleModel);
    removeRolesFromCurrentConfig(roleModel);
  }

  /**
   * removes the given role from the current configuration of all resource types
   * 
   * @param roleModel the role to remove
   */
  private void removeRolesFromCurrentConfig(RoleModel roleModel)
  {
    ResourceEndpoint resourceEndpoint = ScimConfiguration.getScimEndpoint(getKeycloakSession(), false);
    if (resourceEndpoint == null)
    {
      // in this case the realm itself was probably just removed
      return;
    }
    resourceEndpoint.getRegisteredResourceTypes().forEach(resourceType -> {
      ResourceTypeAuthorization authorization = resourceType.getFeatures().getAuthorization();

      BiConsumer<Supplier<Set<String>>, Consumer<Set<String>>> replaceRoles = (setSupplier, setConsumer) -> {
        Set<String> setToManipulate = setSupplier.get();
        setToManipulate.removeIf(roleName -> roleModel.getName().equals(roleName));
        setConsumer.accept(setToManipulate);
      };

      replaceRoles.accept(authorization::getRoles, authorization::setRoles);
      replaceRoles.accept(authorization::getRolesCreate, authorization::setRolesCreate);
      replaceRoles.accept(authorization::getRolesGet, authorization::setRolesGet);
      replaceRoles.accept(authorization::getRolesUpdate, authorization::setRolesUpdate);
      replaceRoles.accept(authorization::getRolesDelete, authorization::setRolesDelete);
    });
  }

  /**
   * removes the given role from the database configuration of all stored resource types
   *
   * @param roleModel the role to remove
   */
  private void removeRolesFromDatabase(RoleModel roleModel)
  {
    // unfortunately I cannot do this in a clean way because the keycloak implementation was prematurely calling
    // the flush-method on the entity manager
    removeFromMappingTable("SCIM_ENDPOINT_ROLES", roleModel);
    removeFromMappingTable("SCIM_ENDPOINT_CREATE_ROLES", roleModel);
    removeFromMappingTable("SCIM_ENDPOINT_GET_ROLES", roleModel);
    removeFromMappingTable("SCIM_ENDPOINT_UPDATE_ROLES", roleModel);
    removeFromMappingTable("SCIM_ENDPOINT_DELETE_ROLES", roleModel);
  }

  /**
   * removes all entries from the given mapping table that has an association with the given role
   *
   * @param roleModel the role that was removed
   */
  private void removeFromMappingTable(String mappingTableName, RoleModel roleModel)
  {
    getEntityManager().createNativeQuery("DELETE FROM " + mappingTableName + " WHERE ROLE_ID = '" + roleModel.getId()
                                         + "'")
                      .executeUpdate();
  }

}
