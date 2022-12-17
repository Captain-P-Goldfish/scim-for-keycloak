package de.captaingoldfish.scim.sdk.keycloak.scim.handler.converter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.keycloak.entities.InfoCertBusinessLineEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.InfoCertCountriesEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimAddressEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimCertificatesEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimEmailsEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimEntitlementEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimImsEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimPhonesEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimPhotosEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 16.12.2022
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScimUserToDatabaseConverter
{

  /**
   * @param user the SCIM user model
   * @param userAttributes the database representation into which the values will be entered. This might be a
   *          new instance during creation or an existing instance when updating.
   */
  public static void addScimValuesToDatabaseModel(CustomUser user,
                                                  UserModel userModel,
                                                  ScimUserAttributesEntity userAttributes)
  {
    userAttributes.setExternalId(user.getExternalId().orElse(null));
    userAttributes.setNameFormatted(user.getName().flatMap(Name::getFormatted).orElse(null));
    userAttributes.setGivenName(user.getName().flatMap(Name::getGivenName).orElse(null));
    userAttributes.setMiddleName(user.getName().flatMap(Name::getMiddleName).orElse(null));
    userAttributes.setFamilyName(user.getName().flatMap(Name::getFamilyName).orElse(null));
    userAttributes.setNameHonorificPrefix(user.getName().flatMap(Name::getHonorificPrefix).orElse(null));
    userAttributes.setNameHonorificSuffix(user.getName().flatMap(Name::getHonorificSuffix).orElse(null));
    userAttributes.setDisplayName(user.getDisplayName().orElse(null));
    userAttributes.setNickName(user.getNickName().orElse(null));
    userAttributes.setProfileUrl(user.getProfileUrl().orElse(null));
    userAttributes.setTitle(user.getTitle().orElse(null));
    userAttributes.setUserType(user.getUserType().orElse(null));
    userAttributes.setPreferredLanguage(user.getPreferredLanguage().orElse(null));
    userAttributes.setLocale(user.getLocale().orElse(null));
    userAttributes.setTimezone(user.getTimezone().orElse(null));
    userAttributes.setEmployeeNumber(user.getEnterpriseUser().flatMap(EnterpriseUser::getEmployeeNumber).orElse(null));
    userAttributes.setDepartment(user.getEnterpriseUser().flatMap(EnterpriseUser::getDepartment).orElse(null));
    userAttributes.setCostCenter(user.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).orElse(null));
    userAttributes.setDivision(user.getEnterpriseUser().flatMap(EnterpriseUser::getDivision).orElse(null));
    userAttributes.setOrganization(user.getEnterpriseUser().flatMap(EnterpriseUser::getOrganization).orElse(null));
    userAttributes.setManagerValue(user.getEnterpriseUser()
                                       .flatMap(EnterpriseUser::getManager)
                                       .flatMap(Manager::getValue)
                                       .orElse(null));
    userAttributes.setManagerReference(user.getEnterpriseUser()
                                           .flatMap(EnterpriseUser::getManager)
                                           .flatMap(Manager::getRef)
                                           .orElse(null));
    final boolean isCreateCall = userAttributes.getUserEntity() == null;
    final Instant lastModified = isCreateCall ? Instant.ofEpochMilli(userModel.getCreatedTimestamp()) : Instant.now();
    userAttributes.setLastModified(lastModified);

    List<ScimAddressEntity> addresses = scimAddressToDatabaseAddress(user, userAttributes);
    userAttributes.setAddresses(addresses);

    List<ScimCertificatesEntity> cerificates = scimCertificatesToDatabaseCertificates(user, userAttributes);
    userAttributes.setCertificates(cerificates);

    List<ScimEmailsEntity> emails = scimEmailsToDatabaseEmails(user, userModel, userAttributes);
    userAttributes.setEmails(emails);

    List<ScimEntitlementEntity> entitlements = scimEntitlementsToDatabaseEntitlements(user, userAttributes);
    userAttributes.setEntitlements(entitlements);

    List<ScimImsEntity> ims = scimImsToDatabaseIms(user, userAttributes);
    userAttributes.setInstantMessagingAddresses(ims);

    List<ScimPhonesEntity> phonenumbers = scimPhonenumbersToDatabasePhonenumbers(user, userAttributes);
    userAttributes.setPhoneNumbers(phonenumbers);

    List<ScimPhotosEntity> photos = scimPhotosToDatabasePhotos(user, userAttributes);
    userAttributes.setPhotos(photos);

    List<InfoCertBusinessLineEntity> businessLines = scimBusinessLinesToDatabaseBusinessLines(user, userAttributes);
    userAttributes.setInfoCertBusinessLine(businessLines);

    List<InfoCertCountriesEntity> countries = scimCountriesToDatabaseCountries(user, userAttributes);
    userAttributes.setInfoCertCountries(countries);
  }

  private static List<ScimAddressEntity> scimAddressToDatabaseAddress(CustomUser user,
                                                                      ScimUserAttributesEntity userAttributes)
  {
    List<ScimAddressEntity> addresses = new ArrayList<>();
    user.getAddresses().forEach(address -> {
      addresses.add(ScimAddressEntity.builder()
                                     .formatted(address.getFormatted().orElse(null))
                                     .streetAddress(address.getStreetAddress().orElse(null))
                                     .locality(address.getLocality().orElse(null))
                                     .region(address.getRegion().orElse(null))
                                     .postalCode(address.getPostalCode().orElse(null))
                                     .country(address.getCountry().orElse(null))
                                     .primary(address.isPrimary())
                                     .type(address.getType().orElse(null))
                                     .userAttributes(userAttributes)
                                     .build());
    });
    return addresses;
  }

  private static List<ScimCertificatesEntity> scimCertificatesToDatabaseCertificates(CustomUser user,
                                                                                     ScimUserAttributesEntity userAttributes)
  {
    List<ScimCertificatesEntity> certificates = new ArrayList<>();
    user.getX509Certificates().forEach(certificate -> {
      certificates.add(ScimCertificatesEntity.builder()
                                             .value(certificate.getValue().orElse(null))
                                             .type(certificate.getType().orElse(null))
                                             .display(certificate.getDisplay().orElse(null))
                                             .primary(certificate.isPrimary())
                                             .userAttributes(userAttributes)
                                             .build());
    });
    return certificates;
  }

  /**
   * parses the SCIM representation of a user into its database email representations
   *
   * @param user the user that may have zero or more emails
   * @param userModel the userModel will receive the primary-email if one is present as base-email
   * @param userAttributes the parent for the given emails
   * @return the list of database email representations from the SCIM user
   */
  private static List<ScimEmailsEntity> scimEmailsToDatabaseEmails(CustomUser user,
                                                                   UserModel userModel,
                                                                   ScimUserAttributesEntity userAttributes)
  {
    List<ScimEmailsEntity> emails = new ArrayList<>();
    user.getEmails().forEach(email -> {
      emails.add(ScimEmailsEntity.builder()
                                 .value(email.getValue().orElse(null))
                                 .type(email.getType().orElse(null))
                                 .primary(email.isPrimary())
                                 .userAttributes(userAttributes)
                                 .build());
      if (email.isPrimary())
      {
        userModel.setEmail(email.getValue().orElse(null));
      }
    });
    return emails;
  }

  private static List<ScimEntitlementEntity> scimEntitlementsToDatabaseEntitlements(CustomUser user,
                                                                                    ScimUserAttributesEntity userAttributes)
  {
    List<ScimEntitlementEntity> entitlements = new ArrayList<>();
    user.getEntitlements().forEach(entitlement -> {
      entitlements.add(ScimEntitlementEntity.builder()
                                            .value(entitlement.getValue().orElse(null))
                                            .display(entitlement.getDisplay().orElse(null))
                                            .type(entitlement.getType().orElse(null))
                                            .primary(entitlement.isPrimary())
                                            .userAttributes(userAttributes)
                                            .build());
    });
    return entitlements;
  }

  private static List<ScimImsEntity> scimImsToDatabaseIms(CustomUser user, ScimUserAttributesEntity userAttributes)
  {
    List<ScimImsEntity> instantMessagingAddresses = new ArrayList<>();
    user.getIms().forEach(ims -> {
      instantMessagingAddresses.add(ScimImsEntity.builder()
                                                 .value(ims.getValue().orElse(null))
                                                 .display(ims.getDisplay().orElse(null))
                                                 .type(ims.getType().orElse(null))
                                                 .primary(ims.isPrimary())
                                                 .userAttributes(userAttributes)
                                                 .build());
    });
    return instantMessagingAddresses;
  }

  private static List<ScimPhonesEntity> scimPhonenumbersToDatabasePhonenumbers(CustomUser user,
                                                                               ScimUserAttributesEntity userAttributes)
  {
    List<ScimPhonesEntity> phoneNumbers = new ArrayList<>();
    user.getPhoneNumbers().forEach(phone -> {
      phoneNumbers.add(ScimPhonesEntity.builder()
                                       .value(phone.getValue().orElse(null))
                                       .display(phone.getDisplay().orElse(null))
                                       .type(phone.getType().orElse(null))
                                       .primary(phone.isPrimary())
                                       .userAttributes(userAttributes)
                                       .build());
    });
    return phoneNumbers;
  }

  private static List<ScimPhotosEntity> scimPhotosToDatabasePhotos(CustomUser user,
                                                                   ScimUserAttributesEntity userAttributes)
  {
    List<ScimPhotosEntity> photos = new ArrayList<>();
    user.getPhotos().forEach(photo -> {
      photos.add(ScimPhotosEntity.builder()
                                 .value(photo.getValue().orElse(null))
                                 .display(photo.getDisplay().orElse(null))
                                 .type(photo.getType().orElse(null))
                                 .primary(photo.isPrimary())
                                 .userAttributes(userAttributes)
                                 .build());
    });
    return photos;
  }

  private static List<InfoCertCountriesEntity> scimCountriesToDatabaseCountries(CustomUser user,
                                                                                ScimUserAttributesEntity userAttributes)
  {
    return Optional.ofNullable(user.getCountryUserExtension()).map(extension -> {
      return extension.getCountries()
                      .stream()
                      .map(country -> InfoCertCountriesEntity.builder()
                                                             .country(country)
                                                             .userAttributes(userAttributes)
                                                             .build())
                      .collect(Collectors.toList());
    }).orElseGet(ArrayList::new);
  }

  private static List<InfoCertBusinessLineEntity> scimBusinessLinesToDatabaseBusinessLines(CustomUser user,
                                                                                           ScimUserAttributesEntity userAttributes)
  {
    return Optional.ofNullable(user.getCountryUserExtension()).map(extension -> {
      return extension.getBusinessLine()
                      .stream()
                      .map(businessLine -> InfoCertBusinessLineEntity.builder()
                                                                     .businessLine(businessLine)
                                                                     .userAttributes(userAttributes)
                                                                     .build())
                      .collect(Collectors.toList());
    }).orElseGet(ArrayList::new);
  }
}
