package de.captaingoldfish.scim.sdk.keycloak.scim.handler.converter;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;

import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Address;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Entitlement;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.GroupNode;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Ims;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PersonRole;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PhoneNumber;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Photo;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.ScimX509Certificate;
import de.captaingoldfish.scim.sdk.keycloak.entities.InfoCertBusinessLineEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.InfoCertCountriesEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.UserFiltering;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CountryUserExtension;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 16.12.2022
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DatabaseUserToScimConverter
{

  /**
   * parses a current database representation into its SCIM representation
   *
   * @param userAttributes the database representation of a user
   * @return the SCIM representation of the user
   */
  public static CustomUser databaseUserModelToScimModel(ScimUserAttributesEntity userAttributes)
  {
    UserEntity userEntity = userAttributes.getUserEntity();
    List<Address> addresses = databaseAddressesToScim(userAttributes);
    List<ScimX509Certificate> certificates = databaseCertificatesToScim(userAttributes);
    List<Email> emails = databaseEmailsToScim(userAttributes);
    List<Entitlement> entitlements = databaseEntitlementsToScim(userAttributes);
    List<Ims> ims = databaseImsToScim(userAttributes);
    List<PhoneNumber> phoneNumbers = databasePhoneNumbersToScim(userAttributes);
    List<Photo> photos = databasePhotosToScim(userAttributes);
    List<PersonRole> personRoles = databasePersonRolesToScim(userAttributes);

    Name name = databaseNameToScimName(userAttributes);

    EnterpriseUser enterpriseUser = toScimEnterpriseUser(userAttributes);
    CountryUserExtension countryUserExtension = toCountryUserExtension(userAttributes);
    CustomUser customUser = CustomUser.builder()
                                      .id(userEntity.getId())
                                      .externalId(userAttributes.getExternalId())
                                      .userName(userEntity.getUsername())
                                      .active(userEntity.isEnabled())
                                      .name(name)
                                      .displayName(userAttributes.getDisplayName())
                                      .nickName(userAttributes.getNickName())
                                      .profileUrl(userAttributes.getProfileUrl())
                                      .title(userAttributes.getTitle())
                                      .userType(userAttributes.getUserType())
                                      .preferredLanguage(userAttributes.getPreferredLanguage())
                                      .locale(userAttributes.getLocale())
                                      .timeZone(userAttributes.getTimezone())
                                      .addresses(addresses)
                                      .x509Certificates(certificates)
                                      .emails(emails)
                                      .entitlements(entitlements)
                                      .ims(ims)
                                      .phoneNumbers(phoneNumbers)
                                      .photos(photos)
                                      .roles(personRoles)
                                      .enterpriseUser(enterpriseUser)
                                      .countryUserExtension(countryUserExtension)
                                      .meta(Meta.builder()
                                                .created(Instant.ofEpochMilli(userEntity.getCreatedTimestamp()))
                                                .lastModified(userAttributes.getLastModified())
                                                .resourceType(ResourceTypeNames.USER)
                                                .build())
                                      .build();

    return customUser;
  }

  /**
   * creates a country user extension from the user-attributes and returns null if no data was found
   */
  private static CountryUserExtension toCountryUserExtension(ScimUserAttributesEntity userAttributes)
  {
    CountryUserExtension countryExtension = CountryUserExtension.builder().build();

    Optional.ofNullable(userAttributes.getInfoCertBusinessLine()).filter(list -> !list.isEmpty()).ifPresent(list -> {
      countryExtension.setBusinessLine(list.stream()
                                           .map(InfoCertBusinessLineEntity::getBusinessLine)
                                           .collect(Collectors.toList()));
    });
    Optional.ofNullable(userAttributes.getInfoCertCountries()).filter(list -> !list.isEmpty()).ifPresent(list -> {
      countryExtension.setCountries(list.stream()
                                        .map(InfoCertCountriesEntity::getCountry)
                                        .collect(Collectors.toList()));
    });

    if (countryExtension.isEmpty())
    {
      return null;
    }
    return countryExtension;
  }

  /**
   * returns the name-attributes or null if the name does not have any attributes
   */
  private static Name databaseNameToScimName(ScimUserAttributesEntity userAttributes)
  {
    Name name = Name.builder()
                    .formatted(userAttributes.getNameFormatted())
                    .givenName(userAttributes.getGivenName())
                    .middlename(userAttributes.getMiddleName())
                    .familyName(userAttributes.getFamilyName())
                    .honorificPrefix(userAttributes.getNameHonorificPrefix())
                    .honorificSuffix(userAttributes.getNameHonorificSuffix())
                    .build();

    if (name.isEmpty())
    {
      return null;
    }
    return name;
  }

  /**
   * creates an {@link EnterpriseUser} object from the database representation of a user if the enterprise user
   * attributes are present within the database
   *
   * @param userAttributes the database representation of the user
   * @return the {@link EnterpriseUser} object if attributes of this object are present within the database
   */
  private static EnterpriseUser toScimEnterpriseUser(ScimUserAttributesEntity userAttributes)
  {
    Manager manager = Manager.builder()
                             .value(userAttributes.getManagerValue())
                             .ref(userAttributes.getManagerReference())
                             .build();
    if (manager.isEmpty())
    {
      manager = null;
    }

    EnterpriseUser enterpriseUser = EnterpriseUser.builder()
                                                  .employeeNumber(userAttributes.getEmployeeNumber())
                                                  .department(userAttributes.getDepartment())
                                                  .costCenter(userAttributes.getCostCenter())
                                                  .division(userAttributes.getDivision())
                                                  .organization(userAttributes.getOrganization())
                                                  .manager(manager)
                                                  .build();

    if (enterpriseUser.isEmpty())
    {
      return null;
    }
    return enterpriseUser;
  }

  private static List<Address> databaseAddressesToScim(ScimUserAttributesEntity userAttributes)
  {
    if (userAttributes.getAddresses() == null)
    {
      return Collections.emptyList();
    }
    return userAttributes.getAddresses().stream().map(address -> {
      return Address.builder()
                    .formatted(address.getFormatted())
                    .streetAddress(address.getStreetAddress())
                    .locality(address.getLocality())
                    .region(address.getRegion())
                    .postalCode(address.getPostalCode())
                    .country(address.getCountry())
                    .primary(address.isPrimary())
                    .build();
    }).collect(Collectors.toList());
  }

  private static List<ScimX509Certificate> databaseCertificatesToScim(ScimUserAttributesEntity userAttributes)
  {
    if (userAttributes.getCertificates() == null)
    {
      return Collections.emptyList();
    }
    return userAttributes.getCertificates().stream().map(email -> {
      return ScimX509Certificate.builder()
                                .value(email.getValue())
                                .display(email.getDisplay())
                                .type(email.getType())
                                .primary(email.isPrimary())
                                .build();
    }).collect(Collectors.toList());
  }

  /**
   * parses emails from the database representation into its SCIM representation
   *
   * @param userAttributes the database representation of user that contains zero or more emails
   * @return the SCIM representations of the email
   */
  private static List<Email> databaseEmailsToScim(ScimUserAttributesEntity userAttributes)
  {
    if (userAttributes.getEmails() == null)
    {
      return Collections.emptyList();
    }
    return userAttributes.getEmails().stream().map(email -> {
      return Email.builder().value(email.getValue()).type(email.getType()).primary(email.isPrimary()).build();
    }).collect(Collectors.toList());
  }

  private static List<Entitlement> databaseEntitlementsToScim(ScimUserAttributesEntity userAttributes)
  {
    if (userAttributes.getEntitlements() == null)
    {
      return Collections.emptyList();
    }
    return userAttributes.getEntitlements().stream().map(entitlement -> {
      return Entitlement.builder()
                        .value(entitlement.getValue())
                        .display(entitlement.getDisplay())
                        .type(entitlement.getType())
                        .primary(entitlement.isPrimary())
                        .build();
    }).collect(Collectors.toList());
  }

  private static List<Ims> databaseImsToScim(ScimUserAttributesEntity userAttributes)
  {
    if (userAttributes.getInstantMessagingAddresses() == null)
    {
      return Collections.emptyList();
    }
    return userAttributes.getInstantMessagingAddresses().stream().map(ims -> {
      return Ims.builder()
                .value(ims.getValue())
                .display(ims.getDisplay())
                .type(ims.getType())
                .primary(ims.isPrimary())
                .build();
    }).collect(Collectors.toList());
  }

  private static List<PhoneNumber> databasePhoneNumbersToScim(ScimUserAttributesEntity userAttributes)
  {
    if (userAttributes.getPhoneNumbers() == null)
    {
      return Collections.emptyList();
    }
    return userAttributes.getPhoneNumbers().stream().map(phoneNumber -> {
      return PhoneNumber.builder()
                        .value(phoneNumber.getValue())
                        .display(phoneNumber.getDisplay())
                        .type(phoneNumber.getType())
                        .primary(phoneNumber.isPrimary())
                        .build();
    }).collect(Collectors.toList());
  }

  private static List<Photo> databasePhotosToScim(ScimUserAttributesEntity userAttributes)
  {
    if (userAttributes.getPhotos() == null)
    {
      return Collections.emptyList();
    }
    return userAttributes.getPhotos().stream().map(photo -> {
      return Photo.builder()
                  .value(photo.getValue())
                  .display(photo.getDisplay())
                  .type(photo.getType())
                  .primary(photo.isPrimary())
                  .build();
    }).collect(Collectors.toList());
  }

  private static List<PersonRole> databasePersonRolesToScim(ScimUserAttributesEntity userAttributes)
  {
    if (userAttributes.getPersonRoles() == null)
    {
      return Collections.emptyList();
    }
    return userAttributes.getPersonRoles().stream().map(personRole -> {
      return PersonRole.builder()
                       .value(personRole.getValue())
                       .display(personRole.getDisplay())
                       .type(personRole.getType())
                       .primary(personRole.isPrimary())
                       .build();
    }).collect(Collectors.toList());
  }

  /**
   * will be used to add additional attributes to the user that are read from the database with an extra SQL
   * 
   * @param userFiltering the user filtering that contains the logic to extract the additional attributes
   * @param customUsers the users to which the attributes should be added
   */
  public static void addAdditionalAttributesToUsers(UserFiltering userFiltering, List<CustomUser> customUsers)
  {
    // since we might need to use the userId list at several points we extract them here once and pass them as
    // parameters to all following methods
    List<String> userIds = customUsers.parallelStream().map(user -> user.getId().get()).collect(Collectors.toList());
    addGroupsToUsers(userFiltering, userIds, customUsers);
  }

  /**
   * will add the users groups to the given list of users
   * 
   * @param userFiltering the instance that contains the logic to extract the groups for the given userIds
   * @param userIds the userIds for which the groups should be extracted
   * @param customUsers the users to which the groups should be added
   */
  private static void addGroupsToUsers(UserFiltering userFiltering, List<String> userIds, List<CustomUser> customUsers)
  {
    List<Pair<GroupEntity, String>> orderedUserGroups = userFiltering.getUserGroups(userIds);

    Map<String, List<Pair<GroupEntity, String>>> splittedMap = orderedUserGroups.stream()
                                                                                .parallel()
                                                                                .collect(Collectors.groupingBy(Pair::getRight));

    splittedMap.entrySet().stream().parallel().forEach(entry -> {
      final String userId = entry.getKey();
      CustomUser user = customUsers.stream().filter(u -> u.getId().get().equals(userId)).findAny().get();
      List<GroupNode> groups = entry.getValue()
                                    .stream()
                                    .parallel()
                                    .map(Pair::getLeft)
                                    .map(group -> GroupNode.builder()
                                                           .value(group.getId())
                                                           .display(group.getName())
                                                           .type("direct")
                                                           .build())
                                    .collect(Collectors.toList());
      user.setGroups(groups);
    });
  }
}
