package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering;

import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.BUSINESS_LINE;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.COUNTRIES;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.GROUPS_ENTITY;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.SCIM_ADDRESSES;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.SCIM_CERTIFICATES;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.SCIM_EMAILS;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.SCIM_ENTITLEMENTS;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.SCIM_IMS;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.SCIM_PERSON_ROLES;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.SCIM_PHONE_NUMBERS;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.SCIM_PHOTOS;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.SCIM_USER_ATTRIBUTES;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.USER_ENTITY;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.USER_GROUPS_MEMBERSHIP;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames.RFC7643;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.AbstractAttributeMapping;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpqlTableJoin;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CountryUserExtension;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;


/**
 * @author Pascal Knueppel
 * @since 12.12.2022
 */
public class UserAttributeMapping extends AbstractAttributeMapping
{

  private static final UserAttributeMapping USER_ATTRIBUTE_MAPPING = new UserAttributeMapping();


  private UserAttributeMapping()
  {
    /*
     * @formatter:off
     * 
     * these joins will all together represent a sql like this:
     * 
     * select distinct u, ua from UserEntity u 
     *   left join ScimUserAttributesEntity ua on u.id = ua.userEntity.id
     *   left join ScimAddressEntity uad on ua.id = uad.userAttributes.id
     *   left join ScimCertificatesEntity uc on ua.id = uc.userAttributes.id
     *   left join ScimEmailsEntity ue on ua.id = ue.userAttributes.id
     *   left join ScimEntitlementEntity uen on ua.id = uen.userAttributes.id
     *   left join ScimImsEntity ui on ua.id = ui.userAttributes.id
     *   left join ScimPhonesEntity uphone on ua.id = uphone.userAttributes.id
     *   left join ScimPhotosEntity uphoto on ua.id = uphoto.userAttributes.id
     *   left join ScimPersonRoleEntity prole on ua.id = prole.userAttributes.id
     *   left join InfoCertCountriesEntity ic on ua.id = ic.userAttributes.id
     *   left join InfoCertBusinessLineEntity ib on ua.id = ib.userAttributes.id
     *   left join UserGroupMembershipEntity ugm on u.id = ugm.user.id
     *   left join GroupEntity g on g.id = ugm.groupId '
     *   left join GroupEntity g on g.id = ugm.groupId '
     *
     * @formatter:on
     */
    JpqlTableJoin baseJpqlTableJoin = new JpqlTableJoin(USER_ENTITY);
    JpqlTableJoin userAttributesJoin = new JpqlTableJoin(USER_ENTITY, SCIM_USER_ATTRIBUTES, "id", "userEntity.id",
                                                         true);
    JpqlTableJoin addressesJoin = new JpqlTableJoin(SCIM_USER_ATTRIBUTES, SCIM_ADDRESSES, "id", "userAttributes.id",
                                                    false);
    JpqlTableJoin certificatesJoin = new JpqlTableJoin(SCIM_USER_ATTRIBUTES, SCIM_CERTIFICATES, "id",
                                                       "userAttributes.id", false);
    JpqlTableJoin emailsJoin = new JpqlTableJoin(SCIM_USER_ATTRIBUTES, SCIM_EMAILS, "id", "userAttributes.id", false);
    JpqlTableJoin entitlementJoin = new JpqlTableJoin(SCIM_USER_ATTRIBUTES, SCIM_ENTITLEMENTS, "id",
                                                      "userAttributes.id", false);
    JpqlTableJoin imsJoin = new JpqlTableJoin(SCIM_USER_ATTRIBUTES, SCIM_IMS, "id", "userAttributes.id", false);
    JpqlTableJoin phonesJoin = new JpqlTableJoin(SCIM_USER_ATTRIBUTES, SCIM_PHONE_NUMBERS, "id", "userAttributes.id",
                                                 false);
    JpqlTableJoin photosJoin = new JpqlTableJoin(SCIM_USER_ATTRIBUTES, SCIM_PHOTOS, "id", "userAttributes.id", false);
    JpqlTableJoin personRolesJoin = new JpqlTableJoin(SCIM_USER_ATTRIBUTES, SCIM_PERSON_ROLES, "id",
                                                      "userAttributes.id", false);
    JpqlTableJoin groupMembershipJoin = new JpqlTableJoin(USER_ENTITY, USER_GROUPS_MEMBERSHIP, "id", "user.id", false);
    JpqlTableJoin groupsJoin = new JpqlTableJoin(USER_GROUPS_MEMBERSHIP, GROUPS_ENTITY, "groupId", "id", false);

    JpqlTableJoin businessLineJoin = new JpqlTableJoin(SCIM_USER_ATTRIBUTES, BUSINESS_LINE, "id", "userAttributes.id",
                                                       false);
    JpqlTableJoin countriesJoin = new JpqlTableJoin(SCIM_USER_ATTRIBUTES, COUNTRIES, "id", "userAttributes.id", false);

    /* Attributes on UserEntity */
    addAttribute(SchemaUris.USER_URI, null, RFC7643.USER_NAME, "username", baseJpqlTableJoin);
    addAttribute(SchemaUris.USER_URI, null, RFC7643.ACTIVE, "enabled", baseJpqlTableJoin);
    addAttribute(SchemaUris.META, RFC7643.META, RFC7643.CREATED, "createdTimestamp", baseJpqlTableJoin);

    /* Attributes on ScimUserAttributesEntity */
    addAttribute(SchemaUris.USER_URI, null, RFC7643.EXTERNAL_ID, "externalId", userAttributesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.NAME, RFC7643.FORMATTED, "nameFormatted", userAttributesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.NAME, RFC7643.GIVEN_NAME, "givenName", userAttributesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.NAME, RFC7643.MIDDLE_NAME, "middleName", userAttributesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.NAME, RFC7643.FAMILY_NAME, "familyName", userAttributesJoin);
    addAttribute(SchemaUris.USER_URI,
                 RFC7643.NAME,
                 RFC7643.HONORIFIC_PREFIX,
                 "nameHonorificPrefix",
                 userAttributesJoin);
    addAttribute(SchemaUris.USER_URI,
                 RFC7643.NAME,
                 RFC7643.HONORIFIC_SUFFIX,
                 "nameHonorificSuffix",
                 userAttributesJoin);
    addAttribute(SchemaUris.USER_URI, null, RFC7643.DISPLAY_NAME, "displayName", userAttributesJoin);
    addAttribute(SchemaUris.USER_URI, null, RFC7643.NICK_NAME, "nickName", userAttributesJoin);
    addAttribute(SchemaUris.USER_URI, null, RFC7643.PROFILE_URL, "profileUrl", userAttributesJoin);
    addAttribute(SchemaUris.USER_URI, null, RFC7643.TITLE, "title", userAttributesJoin);
    addAttribute(SchemaUris.USER_URI, null, RFC7643.USER_TYPE, "userType", userAttributesJoin);
    addAttribute(SchemaUris.USER_URI, null, RFC7643.PREFERRED_LANGUAGE, "preferredLanguage", userAttributesJoin);
    addAttribute(SchemaUris.USER_URI, null, RFC7643.LOCALE, "locale", userAttributesJoin);
    addAttribute(SchemaUris.USER_URI, null, RFC7643.TIMEZONE, "timezone", userAttributesJoin);
    addAttribute(SchemaUris.META, RFC7643.META, RFC7643.LAST_MODIFIED, "lastModified", userAttributesJoin);

    // @formatter:off
    addAttribute(SchemaUris.USER_URI, RFC7643.ADDRESSES, RFC7643.FORMATTED, "formatted", userAttributesJoin, addressesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.ADDRESSES, RFC7643.STREET_ADDRESS, "streetAddress", userAttributesJoin, addressesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.ADDRESSES, RFC7643.LOCALITY, "locality", userAttributesJoin, addressesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.ADDRESSES, RFC7643.REGION, "region", userAttributesJoin, addressesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.ADDRESSES, RFC7643.POSTAL_CODE, "postalCode", userAttributesJoin, addressesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.ADDRESSES, RFC7643.COUNTRY, "country", userAttributesJoin, addressesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.ADDRESSES, RFC7643.TYPE, "type", userAttributesJoin, addressesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.ADDRESSES, RFC7643.PRIMARY, "primary", userAttributesJoin, addressesJoin);

    addAttribute(SchemaUris.USER_URI, RFC7643.X509_CERTIFICATES, RFC7643.VALUE, "value", userAttributesJoin, certificatesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.X509_CERTIFICATES, RFC7643.TYPE, "type", userAttributesJoin, certificatesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.X509_CERTIFICATES, RFC7643.DISPLAY, "display", userAttributesJoin, certificatesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.X509_CERTIFICATES, RFC7643.PRIMARY, "primary", userAttributesJoin, certificatesJoin);

    addAttribute(SchemaUris.USER_URI, RFC7643.EMAILS, RFC7643.VALUE, "value", userAttributesJoin, emailsJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.EMAILS, RFC7643.TYPE, "type", userAttributesJoin, emailsJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.EMAILS, RFC7643.PRIMARY, "primary", userAttributesJoin, emailsJoin);

    addAttribute(SchemaUris.USER_URI, RFC7643.ENTITLEMENTS, RFC7643.VALUE, "value", userAttributesJoin, entitlementJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.ENTITLEMENTS, RFC7643.TYPE, "type", userAttributesJoin, entitlementJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.ENTITLEMENTS, RFC7643.DISPLAY, "display", userAttributesJoin, entitlementJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.ENTITLEMENTS, RFC7643.PRIMARY, "primary", userAttributesJoin, entitlementJoin);

    addAttribute(SchemaUris.USER_URI, RFC7643.IMS, RFC7643.VALUE, "value", userAttributesJoin, imsJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.IMS, RFC7643.TYPE, "type", userAttributesJoin, imsJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.IMS, RFC7643.DISPLAY, "display", userAttributesJoin, imsJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.IMS, RFC7643.PRIMARY, "primary", userAttributesJoin, imsJoin);

    addAttribute(SchemaUris.USER_URI, RFC7643.PHONE_NUMBERS, RFC7643.VALUE, "value", userAttributesJoin, phonesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.PHONE_NUMBERS, RFC7643.TYPE, "type", userAttributesJoin, phonesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.PHONE_NUMBERS, RFC7643.DISPLAY, "display", userAttributesJoin, phonesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.PHONE_NUMBERS, RFC7643.PRIMARY, "primary", userAttributesJoin, phonesJoin);

    addAttribute(SchemaUris.USER_URI, RFC7643.PHOTOS, RFC7643.VALUE, "value", userAttributesJoin, photosJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.PHOTOS, RFC7643.TYPE, "type", userAttributesJoin, photosJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.PHOTOS, RFC7643.DISPLAY, "display", userAttributesJoin, photosJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.PHOTOS, RFC7643.PRIMARY, "primary", userAttributesJoin, photosJoin);

    addAttribute(SchemaUris.USER_URI, RFC7643.ROLES, RFC7643.VALUE, "value", userAttributesJoin, personRolesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.ROLES, RFC7643.TYPE, "type", userAttributesJoin, personRolesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.ROLES, RFC7643.DISPLAY, "display", userAttributesJoin, personRolesJoin);
    addAttribute(SchemaUris.USER_URI, RFC7643.ROLES, RFC7643.PRIMARY, "primary", userAttributesJoin, personRolesJoin);

    addAttribute(SchemaUris.USER_URI, RFC7643.GROUPS, RFC7643.VALUE, "name", groupMembershipJoin, groupsJoin);
    // @formatter:on

    addAttribute(SchemaUris.ENTERPRISE_USER_URI, null, RFC7643.EMPLOYEE_NUMBER, "employeeNumber", userAttributesJoin);
    addAttribute(SchemaUris.ENTERPRISE_USER_URI, null, RFC7643.DEPARTMENT, "department", userAttributesJoin);
    addAttribute(SchemaUris.ENTERPRISE_USER_URI, null, RFC7643.COST_CENTER, "costCenter", userAttributesJoin);
    addAttribute(SchemaUris.ENTERPRISE_USER_URI, null, RFC7643.DIVISION, "division", userAttributesJoin);
    addAttribute(SchemaUris.ENTERPRISE_USER_URI, null, RFC7643.ORGANIZATION, "organization", userAttributesJoin);
    addAttribute(SchemaUris.ENTERPRISE_USER_URI, RFC7643.MANAGER, RFC7643.VALUE, "managerValue", userAttributesJoin);
    addAttribute(SchemaUris.ENTERPRISE_USER_URI, RFC7643.MANAGER, RFC7643.REF, "managerReference", userAttributesJoin);

    addAttribute(CustomUser.FieldNames.COUNTRY_USER_EXTENSION_URI,
                 null,
                 CountryUserExtension.FieldNames.BUSINESS_LINE,
                 "businessLine",
                 userAttributesJoin,
                 businessLineJoin);
    addAttribute(CustomUser.FieldNames.COUNTRY_USER_EXTENSION_URI,
                 null,
                 CountryUserExtension.FieldNames.COUNTRIES,
                 "country",
                 userAttributesJoin,
                 countriesJoin);
  }

  public static UserAttributeMapping getInstance()
  {
    return USER_ATTRIBUTE_MAPPING;
  }

}
