package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.UserEntity;

import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.keycloak.audit.ScimAdminEventBuilder;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimEmailsEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimKeycloakContext;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.UserFiltering;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 10.12.2022
 */
@Slf4j
public class UserHandler2 extends ResourceHandler<CustomUser>
{

  /**
   * create a new user
   * 
   * @param user the resource to store
   * @param context the current request context that holds additional useful information. This object is never
   *          null
   */
  @Override
  public CustomUser createResource(CustomUser user, Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    final String username = user.getUserName().get();
    if (keycloakSession.users().getUserByUsername(keycloakSession.getContext().getRealm(), username) != null)
    {
      throw new ConflictException("the username '" + username + "' is already taken");
    }
    UserModel userModel = keycloakSession.users().addUser(keycloakSession.getContext().getRealm(), username);
    ScimUserAttributesEntity scimUserAttributes = persistUserInDatabase(user, userModel, keycloakSession);
    CustomUser newUser = databaseUserModelToScimModel(scimUserAttributes);
    {
      ScimAdminEventBuilder adminEventAuditer = ((ScimKeycloakContext)context).getAdminEventAuditer();
      adminEventAuditer.createEvent(OperationType.CREATE,
                                    ResourceType.USER,
                                    String.format("users/%s", userModel.getId()),
                                    newUser);
    }
    log.debug("Created user with username: {}", userModel.getUsername());
    return newUser;
  }

  @Override
  public CustomUser getResource(String id,
                                List<SchemaAttribute> attributes,
                                List<SchemaAttribute> excludedAttributes,
                                Context context)
  {
    return null;
  }

  /**
   * filter resources on database
   * 
   * @param startIndex the start index that has a minimum value of 1. So the given startIndex here will never be
   *          lower than 1
   * @param count the number of entries that should be returned to the client. The minimum value of this value
   *          is 0.
   * @param filter the parsed filter expression if the client has given a filter
   * @param sortBy the attribute value that should be used for sorting
   * @param sortOrder the sort order
   * @param attributes the attributes that should be returned to the client. If the client sends this parameter
   *          the evaluation of these parameters might help to improve database performance by omitting
   *          unnecessary table joins
   * @param excludedAttributes the attributes that should NOT be returned to the client. If the client send this
   *          parameter the evaluation of these parameters might help to improve database performance by
   *          omitting unnecessary table joins
   * @param context the current request context that holds additional useful information. This object is never
   *          null
   * @return
   */
  @Override
  public PartialListResponse<CustomUser> listResources(long startIndex,
                                                       int count,
                                                       FilterNode filter,
                                                       SchemaAttribute sortBy,
                                                       SortOrder sortOrder,
                                                       List<SchemaAttribute> attributes,
                                                       List<SchemaAttribute> excludedAttributes,
                                                       Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    UserFiltering userFiltering = new UserFiltering(keycloakSession, startIndex, count, filter, sortBy, sortOrder);
    long totalResults = userFiltering.countResources();
    List<ScimUserAttributesEntity> userAttributesList = userFiltering.filterResources();
    List<CustomUser> customUsers = userAttributesList.parallelStream()
                                                     .map(this::databaseUserModelToScimModel)
                                                     .collect(Collectors.toList());
    return PartialListResponse.<CustomUser> builder().totalResults(totalResults).resources(customUsers).build();
  }

  @Override
  public CustomUser updateResource(CustomUser resourceToUpdate, Context context)
  {
    return null;
  }

  @Override
  public void deleteResource(String id, Context context)
  {

  }


  /* ******************************************************************************************************** */

  /**
   * saves the SCIM representation of a user into the database
   * 
   * @param user the SCIM representation of a user
   * @param userModel the keycloak representation of a user
   * @param keycloakSession the current keycloak request context
   * @return the saved database representation of the given SCIM user
   */
  private ScimUserAttributesEntity persistUserInDatabase(CustomUser user,
                                                         UserModel userModel,
                                                         KeycloakSession keycloakSession)
  {
    final String givenName = user.getName().flatMap(Name::getGivenName).orElse(null);
    final String familyName = user.getName().flatMap(Name::getFamilyName).orElse(null);
    final boolean userActive = user.isActive().orElse(false);
    userModel.setFirstName(givenName);
    userModel.setLastName(familyName);
    userModel.setEnabled(userActive);

    ScimUserAttributesEntity userAttributes = new ScimUserAttributesEntity();
    userAttributes.setUserEntity(((UserAdapter)userModel).getEntity());
    addScimValuesToDatabaseModel(user, userAttributes);

    List<ScimEmailsEntity> emails = scimEmailsToDatabaseEmails(user, userModel);
    userAttributes.setEmails(emails);

    if (isChangePasswordSupported() && user.getPassword().isPresent())
    {
      setPassword(keycloakSession, user.getPassword().get(), userModel);
    }

    EntityManager entityManager = keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();
    entityManager.persist(userAttributes);
    entityManager.flush();
    return userAttributes;
  }

  /**
   * parses the SCIM representation of a user into its database email representations
   * 
   * @param user the user that may have zero or more emails
   * @param userModel the userModel will receive the primary-email if one is present as base-email
   * @return the list of database email representations from the SCIM user
   */
  private List<ScimEmailsEntity> scimEmailsToDatabaseEmails(CustomUser user, UserModel userModel)
  {
    List<ScimEmailsEntity> emails = new ArrayList<>();
    user.getEmails().forEach(email -> {
      emails.add(ScimEmailsEntity.builder()
                                 .value(email.getValue().orElse(null))
                                 .display(email.getDisplay().orElse(null))
                                 .type(email.getType().orElse(null))
                                 .primary(email.isPrimary())
                                 .build());
      if (email.isPrimary())
      {
        userModel.setEmail(email.getValue().orElse(null));
      }
    });
    return emails;
  }

  /**
   * @param user the SCIM user model
   * @param userAttributes the database representation into which the values will be entered. This might be a
   *          new instance during creation or an existing instance when updating.
   */
  private void addScimValuesToDatabaseModel(CustomUser user, ScimUserAttributesEntity userAttributes)
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
  }

  /**
   * parses a current database representation into its SCIM representation
   * 
   * @param userAttributes the database representation of a user
   * @return the SCIM representation of the user
   */
  private CustomUser databaseUserModelToScimModel(ScimUserAttributesEntity userAttributes)
  {
    UserEntity userEntity = userAttributes.getUserEntity();
    List<Email> emails = databaseEmailsToScimEmails(userAttributes);
    EnterpriseUser enterpriseUser = toScimEnterpriseUser(userAttributes);
    return CustomUser.builder()
                     .id(userEntity.getId())
                     .externalId(userAttributes.getExternalId())
                     .userName(userEntity.getUsername())
                     .active(userEntity.isEnabled())
                     .name(Name.builder()
                               .formatted(userAttributes.getNameFormatted())
                               .givenName(userAttributes.getGivenName())
                               .middlename(userAttributes.getMiddleName())
                               .familyName(userAttributes.getFamilyName())
                               .honorificPrefix(userAttributes.getNameHonorificPrefix())
                               .honorificSuffix(userAttributes.getNameHonorificSuffix())
                               .build())
                     .displayName(userAttributes.getDisplayName())
                     .nickName(userAttributes.getNickName())
                     .profileUrl(userAttributes.getProfileUrl())
                     .title(userAttributes.getTitle())
                     .userType(userAttributes.getUserType())
                     .preferredLanguage(userAttributes.getPreferredLanguage())
                     .locale(userAttributes.getLocale())
                     .timeZone(userAttributes.getTimezone())
                     .emails(emails)
                     .enterpriseUser(enterpriseUser)
                     .meta(Meta.builder()
                               .created(Instant.ofEpochMilli(userEntity.getCreatedTimestamp()))
                               .lastModified(userAttributes.getLastModified())
                               .resourceType(ResourceTypeNames.USER)
                               .build())
                     .build();
  }

  /**
   * creates an {@link EnterpriseUser} object from the database representation of a user if the enterprise user
   * attributes are present within the database
   * 
   * @param userAttributes the database representation of the user
   * @return the {@link EnterpriseUser} object if attributes of this object are present within the database
   */
  private EnterpriseUser toScimEnterpriseUser(ScimUserAttributesEntity userAttributes)
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

  /**
   * parses emails from the database representation into its SCIM representation
   * 
   * @param userAttributes the database representation of user that contains zero or more emails
   * @return the SCIM representations of the email
   */
  private List<Email> databaseEmailsToScimEmails(ScimUserAttributesEntity userAttributes)
  {
    return userAttributes.getEmails().stream().map(email -> {
      return Email.builder()
                  .value(email.getValue())
                  .display(email.getDisplay())
                  .type(email.getType())
                  .primary(email.isPrimary())
                  .build();
    }).collect(Collectors.toList());
  }

  /**
   * this method will set the password of the user
   *
   * @param password the password to set
   * @param userModel the model that should receive the password
   */
  private void setPassword(KeycloakSession keycloakSession, String password, UserModel userModel)
  {
    if (StringUtils.isEmpty(password))
    {
      return;
    }
    UserCredentialModel userCredential = UserCredentialModel.password(password);
    UserCredentialManager credentialManager = keycloakSession.userCredentialManager();
    RealmModel realm = keycloakSession.getContext().getRealm();
    try
    {
      credentialManager.updateCredential(realm, userModel, userCredential);
    }
    catch (ModelException ex)
    {
      // this exception is thrown if the password policy was not matched
      log.debug(ex.getMessage(), ex);
      throw new BadRequestException("password policy not matched");
    }
  }
}
