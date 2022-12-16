package de.captaingoldfish.scim.sdk.keycloak.entities;

import java.util.Optional;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.keycloak.models.utils.KeycloakModelUtils;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * @author Pascal Knueppel
 * @since 16.12.2022
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "SCIM_IMS")
public class ScimImsEntity
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
   * reverse mapping for JPA
   */
  @ManyToOne
  @JoinColumn(name = "SCIM_ATTRIBUTES_ID")
  private ScimUserAttributesEntity userAttributes;

  /**
   * the value of the ims
   */
  @Column(name = "IMS_VALUE")
  private String value;

  /**
   * the display value of the ims
   */
  @Column(name = "IMS_DISPLAY")
  private String display;

  /**
   * the type of the ims
   */
  @Column(name = "IMS_TYPE")
  private String type;

  /**
   * if this is the primary ims or not
   */
  @Column(name = "IS_PRIMARY")
  private boolean primary = false;


  @Builder
  public ScimImsEntity(String id,
                       String value,
                       String display,
                       String type,
                       boolean primary,
                       ScimUserAttributesEntity userAttributes)
  {
    this.id = Optional.ofNullable(id).orElse(KeycloakModelUtils.generateId());
    this.value = value;
    this.display = display;
    this.type = type;
    this.primary = primary;
    this.userAttributes = userAttributes;
  }
}
