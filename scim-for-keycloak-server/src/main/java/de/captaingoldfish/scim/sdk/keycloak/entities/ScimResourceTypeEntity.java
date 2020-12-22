package de.captaingoldfish.scim.sdk.keycloak.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * @author Pascal Knueppel
 * @since 05.08.2020
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "SCIM_RESOURCE_TYPE")
@NamedQueries({@NamedQuery(name = "getScimResourceType", query = "SELECT rt FROM ScimResourceTypeEntity rt WHERE "
                                                                 + "rt.realmId = :realmId and rt.name = :name"),
               @NamedQuery(name = "removeScimResourceTypes", query = "DELETE FROM ScimResourceTypeEntity rt WHERE "
                                                                     + "rt.realmId = :realmId")})
public class ScimResourceTypeEntity
{

  /**
   * primary key
   */
  @Id
  @Column(name = "ID")
  @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity. This avoids an extra
                               // SQL
  @Setter(AccessLevel.PROTECTED)
  private String id = KeycloakModelUtils.generateId();

  /**
   * JPA version column that is used as ETag on SCIM
   */
  @Version
  @Column(name = "VERSION")
  private long version;

  /**
   * id of the owning realm<br>
   * <b>NOTE:</b><br>
   * this column wasn't set as foreign key to make realm deletion easier
   */
  @Column(name = "REALM_ID")
  private String realmId;

  /**
   * the NAME attribute is actually the main attribute for resource types not the ID attribute
   */
  @Column(name = "NAME", unique = true)
  private String name;

  /**
   * a simple human readable description for this resource type
   */
  @Column(name = "DESCRIPTION")
  private String description;

  /**
   * if this resource type is enabled or disabled
   */
  @Column(name = "ENABLED")
  private boolean enabled = true;

  /**
   * activates the auto filtering feature of the SCIM-SDK
   */
  @Column(name = "AUTO_FILTERING")
  private boolean autoFiltering;

  /**
   * activates the auto sorting feature of the SCIM-SDK
   */
  @Column(name = "AUTO_SORTING")
  private boolean autoSorting;

  /**
   * activates the automatic calculation of eTags for this resource type if no version attribute has been set
   */
  @Column(name = "ETAG_ENABLED")
  private boolean etagEnabled;

  /**
   * disables creation for resources of this resource type
   */
  @Column(name = "DISABLE_CREATE")
  private boolean disableCreate;

  /**
   * disables reading of resources for this resource type
   */
  @Column(name = "DISABLE_GET")
  private boolean disableGet;

  /**
   * disables listing of resources for this resource type
   */
  @Column(name = "DISABLE_LIST")
  private boolean disableList;

  /**
   * disables update and patch for resources of this resource type
   */
  @Column(name = "DISABLE_UPDATE")
  private boolean disableUpdate;

  /**
   * disables deletion for resources of this resource type
   */
  @Column(name = "DISABLE_DELETE")
  private boolean disableDelete;

  /**
   * if access to this resource type requires authentication
   */
  @Column(name = "REQUIRE_AUTHENTICATION")
  private boolean requireAuthentication = true;

  /**
   * the roles to access either endpoint of "create, get, list, update, patch or delete"
   */
  // @formatter:off
  @OneToMany
  @JoinTable(name = "SCIM_ENDPOINT_ROLES",
             joinColumns = {@JoinColumn(name = "SCIM_RESOURCE_TYPE_ID")},
             inverseJoinColumns = {@JoinColumn(name = "ROLE_ID")})
  // @formatter:on
  private List<RoleEntity> endpointRoles;

  /**
   * the roles to access the create endpoint. Nullifies the values of {@link #endpointRoles} if at least one
   * value is present
   */
  // @formatter:off
  @OneToMany
  @JoinTable(name = "SCIM_ENDPOINT_CREATE_ROLES",
             joinColumns = {@JoinColumn(name = "SCIM_RESOURCE_TYPE_ID")},
             inverseJoinColumns = {@JoinColumn(name = "ROLE_ID")})
  // @formatter:on
  private List<RoleEntity> createRoles;

  /**
   * the roles to access the get/list endpoint. Nullifies the values of {@link #endpointRoles} if at least one
   * value is present
   */
  // @formatter:off
  @OneToMany
  @JoinTable(name = "SCIM_ENDPOINT_GET_ROLES",
             joinColumns = {@JoinColumn(name = "SCIM_RESOURCE_TYPE_ID")},
             inverseJoinColumns = {@JoinColumn(name = "ROLE_ID")})
  // @formatter:on
  private List<RoleEntity> getRoles;

  /**
   * the roles to access the update/patch endpoint. Nullifies the values of {@link #endpointRoles} if at least
   * one value is present
   */
  // @formatter:off
  @OneToMany
  @JoinTable(name = "SCIM_ENDPOINT_UPDATE_ROLES",
             joinColumns = {@JoinColumn(name = "SCIM_RESOURCE_TYPE_ID")},
             inverseJoinColumns = {@JoinColumn(name = "ROLE_ID")})
  // @formatter:on
  private List<RoleEntity> updateRoles;

  /**
   * the roles to access the delete endpoint. Nullifies the values of {@link #endpointRoles} if at least one
   * value is present
   */
  // @formatter:off
  @OneToMany
  @JoinTable(name = "SCIM_ENDPOINT_DELETE_ROLES",
             joinColumns = {@JoinColumn(name = "SCIM_RESOURCE_TYPE_ID")},
             inverseJoinColumns = {@JoinColumn(name = "ROLE_ID")})
  // @formatter:on
  private List<RoleEntity> deleteRoles;

  /**
   * when this resource type was created
   */
  @Column(name = "CREATED")
  private Instant created;

  /**
   * when this resource type was last modified
   */
  @Column(name = "LAST_MODIFIED")
  private Instant lastModified;

  @Builder
  public ScimResourceTypeEntity(String realmId,
                                String name,
                                String description,
                                Boolean enabled,
                                boolean autoFiltering,
                                boolean autoSorting,
                                boolean etagEnabled,
                                boolean disableCreate,
                                boolean disableGet,
                                boolean disableList,
                                boolean disableUpdate,
                                boolean disableDelete,
                                Boolean requireAuthentication,
                                List<RoleEntity> endpointRoles,
                                List<RoleEntity> createRoles,
                                List<RoleEntity> getRoles,
                                List<RoleEntity> updateRoles,
                                List<RoleEntity> deleteRoles,
                                Instant created,
                                Instant lastModified)
  {
    this.realmId = realmId;
    this.name = name;
    this.description = description;
    this.enabled = Optional.ofNullable(enabled).orElse(true);
    this.autoFiltering = autoFiltering;
    this.autoSorting = autoSorting;
    this.etagEnabled = etagEnabled;
    this.disableCreate = disableCreate;
    this.disableGet = disableGet;
    this.disableList = disableList;
    this.disableUpdate = disableUpdate;
    this.disableDelete = disableDelete;
    this.requireAuthentication = Optional.ofNullable(requireAuthentication).orElse(true);
    this.endpointRoles = Optional.ofNullable(endpointRoles).orElse(new ArrayList<>());
    this.createRoles = Optional.ofNullable(createRoles).orElse(new ArrayList<>());
    this.getRoles = Optional.ofNullable(getRoles).orElse(new ArrayList<>());
    this.updateRoles = Optional.ofNullable(updateRoles).orElse(new ArrayList<>());
    this.deleteRoles = Optional.ofNullable(deleteRoles).orElse(new ArrayList<>());
    this.created = created;
    this.lastModified = lastModified;
  }
}
