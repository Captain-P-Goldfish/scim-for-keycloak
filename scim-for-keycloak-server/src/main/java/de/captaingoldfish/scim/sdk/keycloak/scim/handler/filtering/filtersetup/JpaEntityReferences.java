package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup;

import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;

import de.captaingoldfish.scim.sdk.keycloak.entities.ScimAddressEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimCertificatesEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimEmailsEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimEntitlementEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimImsEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimPersonRoleEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimPhonesEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimPhotosEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.SmartBusinessLineEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.SmartCountriesEntity;
import lombok.Getter;


/**
 * This class is used to make sure that the same shortcuts for JPQL entity references are used in different
 * places
 * 
 * @author Pascal Knueppel
 * @since 12.12.2022
 */
public enum JpaEntityReferences
{

  USER_ENTITY("u", UserEntity.class),
  SCIM_USER_ATTRIBUTES("ua", ScimUserAttributesEntity.class),
  SCIM_ADDRESSES("uad", ScimAddressEntity.class),
  SCIM_CERTIFICATES("uc", ScimCertificatesEntity.class),
  SCIM_EMAILS("ue", ScimEmailsEntity.class),
  SCIM_ENTITLEMENTS("uen", ScimEntitlementEntity.class),
  SCIM_IMS("ui", ScimImsEntity.class),
  SCIM_PHONE_NUMBERS("uphone", ScimPhonesEntity.class),
  SCIM_PHOTOS("uphoto", ScimPhotosEntity.class),
  SCIM_PERSON_ROLES("prole", ScimPersonRoleEntity.class),
  USER_GROUPS_MEMBERSHIP("ugm", UserGroupMembershipEntity.class),
  GROUPS_ENTITY("g", GroupEntity.class),
  COUNTRIES("ic", SmartCountriesEntity.class),
  BUSINESS_LINE("ib", SmartBusinessLineEntity.class),
  //
  ;

  /**
   * the JPQL identifier that should identify a specific entity
   */
  @Getter
  private String identifier;

  /**
   * the JPA entity class type
   */
  private Class entityType;

  JpaEntityReferences(String identifier, Class entityType)
  {
    this.identifier = identifier;
    this.entityType = entityType;
  }

  /**
   * @return the name of the JPA entity as it must be added into a JPQL query
   */
  public String getTableName()
  {
    return entityType.getSimpleName();
  }
}
