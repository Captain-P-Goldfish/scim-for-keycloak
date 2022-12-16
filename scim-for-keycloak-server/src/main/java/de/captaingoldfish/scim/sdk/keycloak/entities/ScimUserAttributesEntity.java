package de.captaingoldfish.scim.sdk.keycloak.entities;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * @author Pascal Knueppel
 * @since 09.12.2022
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "USER_SCIM_ATTRIBUTES")
@NamedQueries({@NamedQuery(name = ScimUserAttributesEntity.GET_SCIM_USER_ATTRIBUTES_QUERY_NAME, query = "SELECT ua FROM ScimUserAttributesEntity ua WHERE ua.userEntity.id = :userId")})
public class ScimUserAttributesEntity
{

  public static final String GET_SCIM_USER_ATTRIBUTES_QUERY_NAME = "getScimUserAttributes";

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
   * the owner of these SCIM attributes
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "USER_ID")
  private UserEntity userEntity;

  /**
   * an external id from another system
   */
  @Column(name = "EXTERNAL_ID")
  private String externalId;

  /**
   * the formatted name as defined in SCIM
   */
  @Column(name = "NAME_FORMATTED")
  private String nameFormatted;

  /**
   * the family name as defined in SCIM
   */
  @Column(name = "FAMILY_NAME")
  private String familyName;

  /**
   * the given name as defined in SCIM
   */
  @Column(name = "GIVEN_NAME")
  private String givenName;

  /**
   * the middle name as defined in SCIM
   */
  @Column(name = "MIDDLE_NAME")
  private String middleName;

  /**
   * the honorific prefix of the name as defined in SCIM
   */
  @Column(name = "NAME_HONORIFIC_PREFIX")
  private String nameHonorificPrefix;

  /**
   * the honorific suffix of the name as defined in SCIM
   */
  @Column(name = "NAME_HONORIFIC_SUFFIX")
  private String nameHonorificSuffix;

  /**
   * the display name as defined in SCIM
   */
  @Column(name = "DISPLAY_NAME")
  private String displayName;

  /**
   * the nickname as defined in SCIM
   */
  @Column(name = "NICK_NAME")
  private String nickName;

  /**
   * the profile url as defined in SCIM
   */
  @Column(name = "PROFILE_URL")
  private String profileUrl;

  /**
   * the title as defined in SCIM
   */
  @Column(name = "TITLE")
  private String title;

  /**
   * the user type as defined in SCIM
   */
  @Column(name = "USER_TYPE")
  private String userType;

  /**
   * the preferred language as defined in SCIM
   */
  @Column(name = "PREFERRED_LANGUAGE")
  private String preferredLanguage;

  /**
   * the locale as defined in SCIM
   */
  @Column(name = "LOCALE")
  private String locale;

  /**
   * the locale as defined in SCIM
   */
  @Column(name = "TIMEZONE")
  private String timezone;

  /**
   * the enterprise user employee number
   */
  @Column(name = "EP_EMPLOYEE_NUMBER")
  private String employeeNumber;

  /**
   * the enterprise user department
   */
  @Column(name = "EP_DEPARTMENT")
  private String department;

  /**
   * the enterprise user cost center
   */
  @Column(name = "EP_COST_CENTER")
  private String costCenter;

  /**
   * the enterprise user division
   */
  @Column(name = "EP_DIVISION")
  private String division;

  /**
   * the enterprise user organization
   */
  @Column(name = "EP_ORGANIZATION")
  private String organization;

  /**
   * the enterprise user organization
   */
  @Column(name = "EP_MANAGER_VALUE")
  private String managerValue;

  /**
   * the enterprise user organization
   */
  @Column(name = "EP_MANAGER_REFERENCE")
  private String managerReference;

  /**
   * the moment the user was updated the last time
   */
  @Column(name = "LAST_MODIFIED")
  private long lastModified;

  /**
   * list of scim emails
   */
  @OneToMany(mappedBy = "userAttributes", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ScimEmailsEntity> emails;

  @Builder

  public ScimUserAttributesEntity(String id,
                                  UserEntity userEntity,
                                  String externalId,
                                  String nameFormatted,
                                  String familyName,
                                  String givenName,
                                  String middleName,
                                  String nameHonorificPrefix,
                                  String nameHonorificSuffix,
                                  String displayName,
                                  String nickName,
                                  String profileUrl,
                                  String title,
                                  String userType,
                                  String preferredLanguage,
                                  String locale,
                                  String timezone,
                                  String employeeNumber,
                                  String department,
                                  String costCenter,
                                  String division,
                                  String organization,
                                  String managerValue,
                                  String managerReference,
                                  Instant lastModified,
                                  List<ScimEmailsEntity> emails)
  {
    this.id = Optional.ofNullable(id).orElse(KeycloakModelUtils.generateId());
    this.userEntity = userEntity;
    this.externalId = externalId;
    this.nameFormatted = nameFormatted;
    this.familyName = familyName;
    this.givenName = givenName;
    this.middleName = middleName;
    this.nameHonorificPrefix = nameHonorificPrefix;
    this.nameHonorificSuffix = nameHonorificSuffix;
    this.displayName = displayName;
    this.nickName = nickName;
    this.profileUrl = profileUrl;
    this.title = title;
    this.userType = userType;
    this.preferredLanguage = preferredLanguage;
    this.locale = locale;
    this.timezone = timezone;
    this.employeeNumber = employeeNumber;
    this.department = department;
    this.costCenter = costCenter;
    this.division = division;
    this.organization = organization;
    this.managerValue = managerValue;
    this.managerReference = managerReference;
    this.lastModified = Optional.ofNullable(lastModified).map(Instant::toEpochMilli).orElseGet(() -> {
      return Optional.ofNullable(userEntity).map(UserEntity::getCreatedTimestamp).orElse(0L);
    });
    this.emails = Optional.ofNullable(emails).orElseGet(Collections::emptyList);
    Optional.ofNullable(this.emails).ifPresent(mails -> mails.forEach(mail -> mail.setUserAttributes(this)));
  }

  public Instant getLastModified()
  {
    return Instant.ofEpochMilli(lastModified);
  }

  public void setLastModified(Instant lastModified)
  {
    this.lastModified = lastModified.toEpochMilli();
  }
}
