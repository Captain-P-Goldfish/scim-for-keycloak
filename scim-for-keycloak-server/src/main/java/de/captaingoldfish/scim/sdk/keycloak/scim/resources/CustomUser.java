package de.captaingoldfish.scim.sdk.keycloak.scim.resources;

import java.util.List;
import java.util.Set;

import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.User;
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
import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 16.10.2021
 */
@NoArgsConstructor
public class CustomUser extends User
{

  @Builder
  public CustomUser(String id,
                    String externalId,
                    Meta meta,
                    String userName,
                    Name name,
                    String displayName,
                    String nickName,
                    String profileUrl,
                    String title,
                    String userType,
                    String preferredLanguage,
                    String locale,
                    String timeZone,
                    Boolean active,
                    String password,
                    List<Email> emails,
                    List<PhoneNumber> phoneNumbers,
                    List<Ims> ims,
                    List<Photo> photos,
                    List<Address> addresses,
                    List<GroupNode> groups,
                    List<Entitlement> entitlements,
                    List<PersonRole> roles,
                    List<ScimX509Certificate> x509Certificates,
                    EnterpriseUser enterpriseUser,
                    CountryUserExtension countryUserExtension)
  {
    setId(id);
    setCountryUserExtension(countryUserExtension);
    setExternalId(externalId);
    setMeta(meta);
    setUserName(userName);
    setName(name);
    setDisplayName(displayName);
    setNickName(nickName);
    setProfileUrl(profileUrl);
    setTitle(title);
    setUserType(userType);
    setPreferredLanguage(preferredLanguage);
    setLocale(locale);
    setTimezone(timeZone);
    setActive(active);
    setPassword(password);
    setEmails(emails);
    setPhoneNumbers(phoneNumbers);
    setIms(ims);
    setPhotos(photos);
    setAddresses(addresses);
    setGroups(groups);
    setEntitlements(entitlements);
    setRoles(roles);
    setX509Certificates(x509Certificates);
    setEnterpriseUser(enterpriseUser);
  }

  public CountryUserExtension getCountryUserExtension()
  {
    return getObjectAttribute(FieldNames.COUNTRY_USER_EXTENSION_URI, CountryUserExtension.class).orElse(null);
  }

  public void setCountryUserExtension(CountryUserExtension countryUserExtension)
  {
    Set<String> schemas = this.getSchemas();
    if (countryUserExtension != null && countryUserExtension.size() != 0)
    {
      if (!schemas.contains(FieldNames.COUNTRY_USER_EXTENSION_URI))
      {
        schemas.add(FieldNames.COUNTRY_USER_EXTENSION_URI);
      }
    }
    else
    {
      schemas.remove(FieldNames.COUNTRY_USER_EXTENSION_URI);
    }
    setSchemas(schemas);
    setAttribute(FieldNames.COUNTRY_USER_EXTENSION_URI, countryUserExtension);
  }

  public static class FieldNames
  {

    public static final String COUNTRY_USER_EXTENSION_URI = "urn:ietf:params:scim:schemas:extension:country:2.0:User";

  }

  /**
   * let lombok builder inherit from user builder of scim-sdk
   */
  public static class CustomUserBuilder extends UserBuilder
  {

  }
}
