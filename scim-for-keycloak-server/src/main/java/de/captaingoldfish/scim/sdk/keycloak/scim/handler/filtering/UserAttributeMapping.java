package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering;

import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpqlTableShortcuts.SCIM_USER_ATTRIBUTES;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpqlTableShortcuts.USER_ENTITY;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames.RFC7643;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.AbstractAttributeMapping;


/**
 * @author Pascal Knueppel
 * @since 12.12.2022
 */
public class UserAttributeMapping extends AbstractAttributeMapping
{


  public UserAttributeMapping()
  {
    /* Attributes on UserEntity */
    addAttribute(SchemaUris.USER_URI, null, RFC7643.USER_NAME, USER_ENTITY, "username");
    addAttribute(SchemaUris.USER_URI, null, RFC7643.ACTIVE, USER_ENTITY, "enabled");
    addAttribute(SchemaUris.META, RFC7643.META, RFC7643.CREATED, USER_ENTITY, "createdTimestamp");

    /* Attributes on ScimUserAttributesEntity */
    addAttribute(SchemaUris.USER_URI, null, RFC7643.EXTERNAL_ID, SCIM_USER_ATTRIBUTES, "externalId");
    addAttribute(SchemaUris.USER_URI, RFC7643.NAME, RFC7643.FORMATTED, SCIM_USER_ATTRIBUTES, "nameFormatted");
    addAttribute(SchemaUris.USER_URI, RFC7643.NAME, RFC7643.GIVEN_NAME, SCIM_USER_ATTRIBUTES, "givenName");
    addAttribute(SchemaUris.USER_URI, RFC7643.NAME, RFC7643.MIDDLE_NAME, SCIM_USER_ATTRIBUTES, "middleName");
    addAttribute(SchemaUris.USER_URI, RFC7643.NAME, RFC7643.FAMILY_NAME, SCIM_USER_ATTRIBUTES, "familyName");
    addAttribute(SchemaUris.USER_URI,
                 RFC7643.NAME,
                 RFC7643.HONORIFIC_PREFIX,
                 SCIM_USER_ATTRIBUTES,
                 "nameHonorificPrefix");
    addAttribute(SchemaUris.USER_URI,
                 RFC7643.NAME,
                 RFC7643.HONORIFIC_SUFFIX,
                 SCIM_USER_ATTRIBUTES,
                 "nameHonorificSuffix");
    addAttribute(SchemaUris.USER_URI, null, RFC7643.DISPLAY_NAME, SCIM_USER_ATTRIBUTES, "displayName");
    addAttribute(SchemaUris.USER_URI, null, RFC7643.NICK_NAME, SCIM_USER_ATTRIBUTES, "nickName");
    addAttribute(SchemaUris.USER_URI, null, RFC7643.PROFILE_URL, SCIM_USER_ATTRIBUTES, "profileUrl");
    addAttribute(SchemaUris.USER_URI, null, RFC7643.TITLE, SCIM_USER_ATTRIBUTES, "title");
    addAttribute(SchemaUris.USER_URI, null, RFC7643.USER_TYPE, SCIM_USER_ATTRIBUTES, "userType");
    addAttribute(SchemaUris.USER_URI, null, RFC7643.PREFERRED_LANGUAGE, SCIM_USER_ATTRIBUTES, "preferredLanguage");
    addAttribute(SchemaUris.USER_URI, null, RFC7643.LOCALE, SCIM_USER_ATTRIBUTES, "locale");
    addAttribute(SchemaUris.USER_URI, null, RFC7643.TIMEZONE, SCIM_USER_ATTRIBUTES, "timezone");
    addAttribute(SchemaUris.META, RFC7643.META, RFC7643.LAST_MODIFIED, SCIM_USER_ATTRIBUTES, "lastModified");
    addAttribute(SchemaUris.ENTERPRISE_USER_URI, null, RFC7643.EMPLOYEE_NUMBER, SCIM_USER_ATTRIBUTES, "employeeNumber");
    addAttribute(SchemaUris.ENTERPRISE_USER_URI, null, RFC7643.DEPARTMENT, SCIM_USER_ATTRIBUTES, "department");
    addAttribute(SchemaUris.ENTERPRISE_USER_URI, null, RFC7643.COST_CENTER, SCIM_USER_ATTRIBUTES, "costCenter");
    addAttribute(SchemaUris.ENTERPRISE_USER_URI, null, RFC7643.DIVISION, SCIM_USER_ATTRIBUTES, "division");
    addAttribute(SchemaUris.ENTERPRISE_USER_URI, null, RFC7643.ORGANIZATION, SCIM_USER_ATTRIBUTES, "organization");
    addAttribute(SchemaUris.ENTERPRISE_USER_URI, RFC7643.MANAGER, RFC7643.VALUE, SCIM_USER_ATTRIBUTES, "managerValue");
    addAttribute(SchemaUris.ENTERPRISE_USER_URI,
                 RFC7643.MANAGER,
                 RFC7643.REF,
                 SCIM_USER_ATTRIBUTES,
                 "managerReference");
  }

}
