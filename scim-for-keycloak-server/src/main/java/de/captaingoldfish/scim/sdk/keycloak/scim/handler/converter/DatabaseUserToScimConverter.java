package de.captaingoldfish.scim.sdk.keycloak.scim.handler.converter;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.models.jpa.entities.UserEntity;

import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Address;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Entitlement;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Ims;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PhoneNumber;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Photo;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.ScimX509Certificate;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
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

    Name name = databaseNameToScimName(userAttributes);

    EnterpriseUser enterpriseUser = toScimEnterpriseUser(userAttributes);
    return CustomUser.builder()
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
                     .enterpriseUser(enterpriseUser)
                     .meta(Meta.builder()
                               .created(Instant.ofEpochMilli(userEntity.getCreatedTimestamp()))
                               .lastModified(userAttributes.getLastModified())
                               .resourceType(ResourceTypeNames.USER)
                               .build())
                     .build();
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

}
