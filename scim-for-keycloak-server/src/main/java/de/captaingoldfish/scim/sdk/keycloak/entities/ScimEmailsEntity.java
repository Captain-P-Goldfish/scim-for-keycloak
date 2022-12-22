package de.captaingoldfish.scim.sdk.keycloak.entities;

import java.util.Optional;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.keycloak.models.utils.KeycloakModelUtils;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * @author Pascal Knueppel
 * @since 09.12.2022
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "SCIM_EMAILS")
public class ScimEmailsEntity
{

  /**
   * primary key
   */
  @EqualsAndHashCode.Exclude
  @Id
  @Column(name = "ID")
  @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity. This avoids an extra
  // SQL
  @Setter(AccessLevel.PROTECTED)
  private String id = KeycloakModelUtils.generateId();

  /**
   * reverse mapping for JPA
   */
  @EqualsAndHashCode.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "SCIM_ATTRIBUTES_ID")
  private ScimUserAttributesEntity userAttributes;

  /**
   * the value of the email
   */
  @Column(name = "EMAIL_VALUE")
  private String value;

  /**
   * the type of the email
   */
  @Column(name = "EMAIL_TYPE")
  private String type;

  /**
   * if this is the primary email or not
   */
  @Column(name = "IS_PRIMARY")
  private boolean primary = false;


  @Builder
  public ScimEmailsEntity(String id,
                          String value,
                          String type,
                          boolean primary,
                          ScimUserAttributesEntity userAttributes)
  {
    this.id = Optional.ofNullable(id).orElse(KeycloakModelUtils.generateId());
    this.value = value;
    this.type = type;
    this.primary = primary;
    this.userAttributes = userAttributes;
  }
}
