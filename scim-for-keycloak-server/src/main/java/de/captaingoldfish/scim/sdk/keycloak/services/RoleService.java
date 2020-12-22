package de.captaingoldfish.scim.sdk.keycloak.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.RoleAdapter;
import org.keycloak.models.jpa.entities.RoleEntity;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.MultiComplexNode;
import de.captaingoldfish.scim.sdk.keycloak.custom.resources.ChildRole;
import de.captaingoldfish.scim.sdk.keycloak.custom.resources.RealmRole;


/**
 * provides methods to extract, create or update roles in the database
 *
 * @author Pascal Knueppel
 * @since 16.08.2020
 */
public class RoleService extends AbstractService
{

  public RoleService(KeycloakSession keycloakSession)
  {
    super(keycloakSession);
  }

  /**
   * gets a role by its id
   *
   * @param id the id of the role that should be extracted
   * @return an empty if the role does not exist
   */
  public Optional<RoleModel> getRoleModel(String id)
  {
    EntityManager entityManager = getEntityManager();
    TypedQuery<RoleEntity> query = entityManager.createQuery("select role from RoleEntity role "
                                                             + "where role.clientRole = false "
                                                             + "and role.realm.id = :realm and role.id = :id",
                                                             RoleEntity.class);
    try
    {
      RoleEntity roleEntity = query.setParameter("realm", getKeycloakSession().getContext().getRealm().getId())
                                   .setParameter("id", id)
                                   .getSingleResult();
      return Optional.of(new RoleAdapter(getKeycloakSession(), getKeycloakSession().getContext().getRealm(),
                                         entityManager, roleEntity));
    }
    catch (NoResultException ex)
    {
      return Optional.empty();
    }
  }

  /**
   * updates the role with the given scim representation
   *
   * @param role the scim representation of the role that should be returned
   * @return the updated role
   */
  public RoleModel updateRole(RealmRole role)
  {
    String roleId = role.getId().orElse(null); // is never null for SCIM schema validation
    Optional<RoleModel> optionalRole = getRoleModel(roleId);
    if (!optionalRole.isPresent())
    {
      throw new ResourceNotFoundException("role with id '" + roleId + "' does not exist");
    }
    RoleModel roleModel = optionalRole.get();
    role.getDescription().ifPresent(roleModel::setDescription);
    role.getExternalId()
        .ifPresent(externalId -> roleModel.setSingleAttribute(AttributeNames.RFC7643.EXTERNAL_ID, externalId));

    removeRolesFromAllAssociates(roleModel);
    role.getAssociates().forEach(associate -> addRoleToAssociate(roleModel, associate));

    removeAllChildRoles(roleModel);
    role.getChildren().forEach(childRole -> addChildRole(roleModel, childRole));

    roleModel.setAttribute(AttributeNames.RFC7643.LAST_MODIFIED, Collections.singleton(Instant.now().toString()));
    return roleModel;
  }

  /**
   * removes all child roles from the given role since SCIM update overrides the whole resource
   *
   * @param roleModel the role model that is being stripped of its composite role associates
   */
  private void removeAllChildRoles(RoleModel roleModel)
  {
    roleModel.getComposites().forEach(roleModel::removeCompositeRole);
  }

  /**
   * adds the given child role to this role
   *
   * @param roleModel the role that gets a child role added
   * @param childRole the child role that is being added to its parent role
   */
  private void addChildRole(RoleModel roleModel, ChildRole childRole)
  {
    RealmModel realmModel = getKeycloakSession().getContext().getRealm();
    final String childRoleId = childRole.getValue().orElse(null);
    RoleModel childRoleModel = getKeycloakSession().realms().getRoleById(childRoleId, realmModel);
    if (childRoleModel == null)
    {
      throw new ResourceNotFoundException("role with id '" + childRoleId + "' does not exist");
    }
    roleModel.addCompositeRole(childRoleModel);
  }

  /**
   * since a update operation in SCIM is a complete new setting for the resource the role must be removed from
   * the previous associated resources
   *
   * @param roleModel the role that should be removed from all its associates
   */
  private void removeRolesFromAllAssociates(RoleModel roleModel)
  {
    getAssociatedMembers(roleModel).forEach(roleMapperModel -> {
      roleMapperModel.deleteRoleMapping(roleModel);
    });
  }

  /**
   * creates a new role by the given SCIM role representation
   *
   * @param role the new role
   * @return the newly created role
   */
  public RoleModel createNewRole(RealmRole role)
  {
    RealmModel realmModel = getKeycloakSession().getContext().getRealm();
    RoleModel roleModel = getKeycloakSession().realms().addRealmRole(realmModel, role.getName());
    role.getDescription().ifPresent(roleModel::setDescription);
    role.getExternalId()
        .ifPresent(externalId -> roleModel.setSingleAttribute(AttributeNames.RFC7643.EXTERNAL_ID, externalId));
    role.getAssociates().forEach(associate -> {
      addRoleToAssociate(roleModel, associate);
    });
    role.getChildren().forEach(child -> {
      addChildRole(roleModel, child);
    });
    Instant now = Instant.now();
    roleModel.setAttribute(AttributeNames.RFC7643.CREATED, Collections.singleton(now.toString()));
    roleModel.setAttribute(AttributeNames.RFC7643.LAST_MODIFIED, Collections.singleton(now.toString()));
    return roleModel;
  }

  /**
   * adds the given role to the given associate
   *
   * @param roleModel the role that should be added to the given associate
   * @param associate the associate to which the role should be added
   */
  private void addRoleToAssociate(RoleModel roleModel, MultiComplexNode associate)
  {
    String typeAttribute = associate.getType().orElse(null); // is never null for SCIM schema validation
    String associateId = associate.getValue().orElse(null); // is never null for SCIM schema validation
    if (ResourceTypeNames.USER.equals(typeAttribute))
    {
      addRoleToUser(roleModel, associateId);
    }
    else if (ResourceTypeNames.GROUPS.equals(typeAttribute))
    {
      addRoleToGroup(roleModel, associateId);
    }
    else
    {
      throw new BadRequestException("resource type '" + typeAttribute + "' is not valid as role associate");
    }
  }

  /**
   * adds the given role to a keycloak user
   *
   * @param roleModel the role that should be added to the user
   * @param userId the id of the user
   */
  private void addRoleToUser(RoleModel roleModel, String userId)
  {
    UserModel userModel = getKeycloakSession().users()
                                              .getUserById(userId, getKeycloakSession().getContext().getRealm());
    if (userModel == null)
    {
      throw new ResourceNotFoundException("role cannot be granted to non existing user with id '" + userId + "'");
    }
    userModel.grantRole(roleModel);
  }

  /**
   * adds the given role to a keycloak group
   *
   * @param roleModel the role that should be added to the group
   * @param groupId the id of the group
   */
  private void addRoleToGroup(RoleModel roleModel, String groupId)
  {
    GroupModel groupModel = getKeycloakSession().realms()
                                                .getGroupById(groupId, getKeycloakSession().getContext().getRealm());
    if (groupModel == null)
    {
      throw new ResourceNotFoundException("role cannot be granted to non existing group with id '" + groupId + "'");
    }
    groupModel.grantRole(roleModel);
  }

  /**
   * @param roleModel the role of which the associated members should be found
   * @return the members that are associated with the given role
   */
  public List<RoleMapperModel> getAssociatedMembers(RoleModel roleModel)
  {
    RealmModel realmModel = getKeycloakSession().getContext().getRealm();
    List<UserModel> userRoleMembers = getKeycloakSession().users().getRoleMembers(realmModel, roleModel);
    List<GroupModel> groupRoleMembers = getKeycloakSession().realms().getGroupsByRole(realmModel, roleModel, -1, -1);
    List<RoleMapperModel> roleMappers = new ArrayList<>(userRoleMembers);
    roleMappers.addAll(groupRoleMembers);
    return roleMappers;
  }
}
