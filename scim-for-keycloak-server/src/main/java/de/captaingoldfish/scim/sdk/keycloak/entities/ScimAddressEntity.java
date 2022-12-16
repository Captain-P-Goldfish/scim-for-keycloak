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
@Table(name = "SCIM_ADDRESSES")
public class ScimAddressEntity
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
   * the formatted address
   */
  @Column(name = "FORMATTED")
  private String formatted;

  /**
   * the street and number of the address
   */
  @Column(name = "STREET_ADDRESS")
  private String streetAddress;

  /**
   * the locality of the address
   */
  @Column(name = "LOCALITY")
  private String locality;

  /**
   * the region of the address
   */
  @Column(name = "REGION")
  private String region;

  /**
   * the postal code of the address
   */
  @Column(name = "POSTALCODE")
  private String postalCode;

  /**
   * the country of the address
   */
  @Column(name = "COUNTRY")
  private String country;

  /**
   * the addresses' type
   */
  @Column(name = "ADDRESS_TYPE")
  private String type;

  /**
   * if this is the primary ims or not
   */
  @Column(name = "IS_PRIMARY")
  private boolean primary = false;


  @Builder

  public ScimAddressEntity(String id,
                           String formatted,
                           String streetAddress,
                           String locality,
                           String region,
                           String postalCode,
                           String country,
                           String type,
                           boolean primary,
                           ScimUserAttributesEntity userAttributes)
  {
    Optional.ofNullable(id).orElse(KeycloakModelUtils.generateId());
    this.userAttributes = userAttributes;
    this.formatted = formatted;
    this.streetAddress = streetAddress;
    this.locality = locality;
    this.region = region;
    this.postalCode = postalCode;
    this.country = country;
    this.type = type;
    this.primary = primary;
  }
}
