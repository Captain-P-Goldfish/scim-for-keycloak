package de.captaingoldfish.scim.sdk.keycloak.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.UserEntity;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Address;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Entitlement;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Ims;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.MultiComplexNode;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PersonRole;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PhoneNumber;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Photo;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.ScimX509Certificate;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.UserLegacyHandler;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.converter.ScimUserToDatabaseConverter;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CountryUserExtension;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 22.12.2022
 */
@Slf4j
public class UserLegacyDataMigrationService extends AbstractService
{

  public UserLegacyDataMigrationService(KeycloakSession keycloakSession)
  {
    super(keycloakSession);
  }

  /**
   * will migrate the user-data from the legacy endpoint to the new endpoint <br>
   * <br>
   * for the migration process we cannot use the keycloakSession since we need to migrate all users from all
   * realms and the keycloak session methods are always bound to a specific realm.
   */
  public void migrateUserData()
  {
    EntityManager entityManager = getKeycloakSession().getProvider(JpaConnectionProvider.class).getEntityManager();
    List<UserEntity> userEntities = entityManager.createQuery("select u from UserEntity u", UserEntity.class)
                                                 .getResultList();

    final Map<String, RealmModel> alreadyLoadedRealms = new HashMap<>();
    Function<String, RealmModel> getRealm = realmId -> {
      RealmModel realmModel = alreadyLoadedRealms.get(realmId);
      if (realmModel == null)
      {
        realmModel = getKeycloakSession().realms().getRealm(realmId);
        alreadyLoadedRealms.put(realmId, realmModel);
      }
      return realmModel;
    };

    List<String> failedUserMigrations = new ArrayList<>();
    List<String> omittedUserMigrations = new ArrayList<>();

    for ( UserEntity userEntity : userEntities )
    {
      RealmModel realmModel = getRealm.apply(userEntity.getRealmId());
      log.info("Migrating user '{}' of realm '{}'", userEntity.getUsername(), realmModel.getName());
      ScimUserAttributesEntity attributesEntity = findUserById(userEntity.getId());
      if (attributesEntity != null)
      {
        log.info("user '{}' does already have SCIM data and is omitted from migration", userEntity.getUsername());
        omittedUserMigrations.add(userEntity.getUsername());
        continue;
      }
      entityManager.merge(userEntity);
      UserModel userModel = new UserAdapter(getKeycloakSession(), realmModel, entityManager, userEntity);

      CustomUser scimRepresentation = legacyUserToUser(userModel);
      ScimUserAttributesEntity userAttributes = new ScimUserAttributesEntity();
      ScimUserToDatabaseConverter.addScimValuesToDatabaseModel(scimRepresentation, userModel, userAttributes);

      boolean isEmptyObject = userAttributes.equals(new ScimUserAttributesEntity());
      if (!isEmptyObject)
      {
        removeAllScimAttributesFromUserAttributesTable(userModel);
        entityManager.persist(userAttributes);
        userAttributes.setUserEntity(userEntity);
      }

      try
      {
        entityManager.flush();
      }
      catch (Exception ex)
      {
        log.error("Migration failed for user: {}", userModel.getUsername());
        log.error("userRepresentation for debugging purposes: {}", scimRepresentation);
        log.error(ex.getMessage(), ex);
        failedUserMigrations.add(userModel.getUsername());
      }
    }

    if (omittedUserMigrations.size() > 0)
    {
      log.warn("The following users were omitted from migration because they already have SCIM data-sets: {}",
               omittedUserMigrations);
    }
    if (failedUserMigrations.size() > 0)
    {
      log.error("The following users could unexpectedly not be migrated: {}", failedUserMigrations);
    }
    else
    {
      log.info("User migration successfully executed");
    }
  }

  private ScimUserAttributesEntity findUserById(String userId)
  {
    EntityManager entityManager = getKeycloakSession().getProvider(JpaConnectionProvider.class).getEntityManager();
    final String queryName = ScimUserAttributesEntity.GET_SCIM_USER_ATTRIBUTES_QUERY_NAME;
    Query query = entityManager.createNamedQuery(queryName);
    query.setParameter("userId", userId);
    try
    {
      Object[] result = (Object[])query.getSingleResult();
      if (result[1] == null)
      {
        return null;
      }
      return (ScimUserAttributesEntity)result[1];
    }
    catch (NoResultException ex)
    {
      log.debug(ex.getMessage());
      // causes a 404 not found exception
      return null;
    }
  }

  /**
   * if the data was migrated to the new table structure we will delete it from the user-attributes table
   */
  private void removeAllScimAttributesFromUserAttributesTable(UserModel userModel)
  {
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.MIDDLE_NAME);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.HONORIFIC_PREFIX);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.HONORIFIC_SUFFIX);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.FORMATTED);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.EXTERNAL_ID);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.NICK_NAME);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.TITLE);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.DISPLAY_NAME);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.USER_TYPE);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.LOCALE);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.PREFERRED_LANGUAGE);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.TIMEZONE);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.PROFILE_URL);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.EMAILS);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.PHONE_NUMBERS);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.ADDRESSES);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.IMS);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.ENTITLEMENTS);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.PHOTOS);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.ROLES);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.X509_CERTIFICATES);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.LAST_MODIFIED);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.MANAGER);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.COST_CENTER);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.DEPARTMENT);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.DIVISION);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.EMPLOYEE_NUMBER);
    removeScimAttributeFromUserAttributesTable(userModel, AttributeNames.RFC7643.ORGANIZATION);
    removeScimAttributeFromUserAttributesTable(userModel,
                                               String.format("%s:%s",
                                                             CustomUser.FieldNames.COUNTRY_USER_EXTENSION_URI,
                                                             CountryUserExtension.FieldNames.COUNTRIES));
    removeScimAttributeFromUserAttributesTable(userModel,
                                               String.format("%s:%s",
                                                             CustomUser.FieldNames.COUNTRY_USER_EXTENSION_URI,
                                                             CountryUserExtension.FieldNames.BUSINESS_LINE));
  }

  private void removeScimAttributeFromUserAttributesTable(UserModel userModel, String attributeName)
  {
    userModel.removeAttribute(attributeName);
    userModel.removeAttribute(attributeName + UserLegacyHandler.PRIMARY_SUFFIX);
  }

  private CustomUser legacyUserToUser(UserModel userModel)
  {
    List<Email> emails = getAttributeList(Email.class, AttributeNames.RFC7643.EMAILS, userModel);

    Optional.ofNullable(userModel.getEmail()).ifPresent(email -> {
      // remove emails that are marked as primary in favor of the email property attribute of the user
      emails.removeIf(MultiComplexNode::isPrimary);
      emails.add(Email.builder().primary(true).value(email).build());
    });

    Name name = Name.builder()
                    .givenName(userModel.getFirstName())
                    .familyName(userModel.getLastName())
                    .middlename(userModel.getFirstAttribute(AttributeNames.RFC7643.MIDDLE_NAME))
                    .honorificPrefix(userModel.getFirstAttribute(AttributeNames.RFC7643.HONORIFIC_PREFIX))
                    .honorificSuffix(userModel.getFirstAttribute(AttributeNames.RFC7643.HONORIFIC_SUFFIX))
                    .formatted(userModel.getFirstAttribute(AttributeNames.RFC7643.FORMATTED))
                    .build();
    if (name.isEmpty())
    {
      name = null;
    }

    CountryUserExtension countryUserExtension = getCountryUserExtension(userModel);
    CustomUser user = CustomUser.builder()
                                .countryUserExtension(countryUserExtension)
                                .id(userModel.getId())
                                .externalId(userModel.getFirstAttribute(AttributeNames.RFC7643.EXTERNAL_ID))
                                .userName(userModel.getUsername())
                                .name(name)
                                .active(userModel.isEnabled())
                                .nickName(userModel.getFirstAttribute(AttributeNames.RFC7643.NICK_NAME))
                                .title(userModel.getFirstAttribute(AttributeNames.RFC7643.TITLE))
                                .displayName(userModel.getFirstAttribute(AttributeNames.RFC7643.DISPLAY_NAME))
                                .userType(userModel.getFirstAttribute(AttributeNames.RFC7643.USER_TYPE))
                                .locale(userModel.getFirstAttribute(AttributeNames.RFC7643.LOCALE))
                                .preferredLanguage(userModel.getFirstAttribute(AttributeNames.RFC7643.PREFERRED_LANGUAGE))
                                .timeZone(userModel.getFirstAttribute(AttributeNames.RFC7643.TIMEZONE))
                                .profileUrl(userModel.getFirstAttribute(AttributeNames.RFC7643.PROFILE_URL))
                                .emails(emails)
                                .phoneNumbers(getAttributeList(PhoneNumber.class,
                                                               AttributeNames.RFC7643.PHONE_NUMBERS,
                                                               userModel))
                                .addresses(getAttributeList(Address.class, AttributeNames.RFC7643.ADDRESSES, userModel))
                                .ims(getAttributeList(Ims.class, AttributeNames.RFC7643.IMS, userModel))
                                .entitlements(getAttributeList(Entitlement.class,
                                                               AttributeNames.RFC7643.ENTITLEMENTS,
                                                               userModel))
                                .photos(getAttributeList(Photo.class, AttributeNames.RFC7643.PHOTOS, userModel))
                                .roles(getAttributeList(PersonRole.class, AttributeNames.RFC7643.ROLES, userModel))
                                .x509Certificates(getAttributeList(ScimX509Certificate.class,
                                                                   AttributeNames.RFC7643.X509_CERTIFICATES,
                                                                   userModel))
                                .meta(Meta.builder()
                                          .created(Optional.ofNullable(userModel.getCreatedTimestamp())
                                                           .map(Instant::ofEpochMilli)
                                                           .orElseGet(() -> {
                                                             log.warn("CustomUser with ID '{}' has no created timestamp",
                                                                      userModel.getId());
                                                             return Instant.now();
                                                           }))
                                          .lastModified(getLastModified(userModel))
                                          .build())
                                .build();

    Manager manager = Manager.builder().value(userModel.getFirstAttribute(AttributeNames.RFC7643.MANAGER)).build();
    EnterpriseUser enterpriseUser = EnterpriseUser.builder()
                                                  .costCenter(userModel.getFirstAttribute(AttributeNames.RFC7643.COST_CENTER))
                                                  .department(userModel.getFirstAttribute(AttributeNames.RFC7643.DEPARTMENT))
                                                  .division(userModel.getFirstAttribute(AttributeNames.RFC7643.DIVISION))
                                                  .employeeNumber(userModel.getFirstAttribute(AttributeNames.RFC7643.EMPLOYEE_NUMBER))
                                                  .organization(userModel.getFirstAttribute(AttributeNames.RFC7643.ORGANIZATION))
                                                  .build();
    if (!manager.isEmpty())
    {
      enterpriseUser.setManager(manager);
    }
    if (!enterpriseUser.isEmpty())
    {
      user.setEnterpriseUser(enterpriseUser);
    }
    return user;
  }

  private <T extends MultiComplexNode> List<T> getAttributeList(Class<T> type,
                                                                String attributeName,
                                                                UserModel keycloakUser)
  {
    List<T> attributeList = new ArrayList<>();
    keycloakUser.getAttributeStream(attributeName).forEach(attribute -> {
      attributeList.add(JsonHelper.readJsonDocument(attribute, type));
    });
    keycloakUser.getAttributeStream(attributeName + UserLegacyHandler.PRIMARY_SUFFIX).forEach(attribute -> {
      attributeList.add(JsonHelper.readJsonDocument(attribute, type));
    });
    return attributeList;
  }

  /**
   * gets the lastModified value of the user
   *
   * @param userEntity the user model from which the last modified value should be extracted
   * @return the last modified value of the given user
   */
  private Instant getLastModified(UserModel userEntity)
  {
    String lastModifiedString = userEntity.getFirstAttribute(AttributeNames.RFC7643.LAST_MODIFIED);
    if (StringUtils.isNotBlank(lastModifiedString))
    {
      return Instant.ofEpochMilli(Long.parseLong(lastModifiedString));
    }
    else
    {
      return Optional.ofNullable(userEntity.getCreatedTimestamp()).map(Instant::ofEpochMilli).orElse(Instant.now());
    }
  }

  private CountryUserExtension getCountryUserExtension(UserModel userModel)
  {
    List<String> countries = userModel.getAttributeStream(String.format("%s:%s",
                                                                        CustomUser.FieldNames.COUNTRY_USER_EXTENSION_URI,
                                                                        CountryUserExtension.FieldNames.COUNTRIES))
                                      .collect(Collectors.toList());
    List<String> businessLines = userModel.getAttributeStream(String.format("%s:%s",
                                                                            CustomUser.FieldNames.COUNTRY_USER_EXTENSION_URI,
                                                                            CountryUserExtension.FieldNames.BUSINESS_LINE))
                                          .collect(Collectors.toList());
    CountryUserExtension countryUserExtension = CountryUserExtension.builder()
                                                                    .countries(countries)
                                                                    .businessLine(businessLines)
                                                                    .build();
    if (countryUserExtension.isEmpty())
    {
      countryUserExtension = null;
    }
    return countryUserExtension;
  }
}
