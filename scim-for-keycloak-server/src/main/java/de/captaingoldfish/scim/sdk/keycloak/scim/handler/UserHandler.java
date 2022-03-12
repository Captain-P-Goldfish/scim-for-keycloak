package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Address;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Entitlement;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.GroupNode;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Ims;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.MultiComplexNode;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PersonRole;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PhoneNumber;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Photo;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.ScimX509Certificate;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.audit.ScimAdminEventBuilder;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimKeycloakContext;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 04.02.2020 <br>
 * <br>
 */
@Slf4j
public class UserHandler extends ResourceHandler<User>
{

  public static final String PRIMARY_SUFFIX = "_primary";

  /**
   * {@inheritDoc}
   */
  @Override
  public User createResource(User user, Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    final String username = user.getUserName().get();
    if (keycloakSession.users().getUserByUsername(keycloakSession.getContext().getRealm(), username) != null)
    {
      throw new ConflictException("the username '" + username + "' is already taken");
    }
    UserModel userModel = keycloakSession.users().addUser(keycloakSession.getContext().getRealm(), username);
    userModel = userToModel(user, userModel);
    if (isChangePasswordSupported() && user.getPassword().isPresent())
    {
      setPassword(keycloakSession, user.getPassword().get(), userModel);
    }
    User newUser = modelToUser(userModel);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public User getResource(String id,
                          List<SchemaAttribute> attributes,
                          List<SchemaAttribute> excludedAttributes,
                          Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    UserModel userModel = keycloakSession.users().getUserById(keycloakSession.getContext().getRealm(), id);
    if (userModel == null)
    {
      return null; // causes a resource not found exception you may also throw it manually
    }
    return modelToUser(userModel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PartialListResponse<User> listResources(long startIndex,
                                                 int count,
                                                 FilterNode filter,
                                                 SchemaAttribute sortBy,
                                                 SortOrder sortOrder,
                                                 List<SchemaAttribute> attributes,
                                                 List<SchemaAttribute> excludedAttributes,
                                                 Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    // TODO in order to filter on database level the feature "autoFiltering" must be disabled and the JPA criteria
    // api should be used
    RealmModel realmModel = keycloakSession.getContext().getRealm();
    Stream<UserModel> userModels = keycloakSession.users().getUsersStream(realmModel);
    List<User> userList = userModels.map(this::modelToUser).collect(Collectors.toList());
    return PartialListResponse.<User> builder().totalResults(userList.size()).resources(userList).build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public User updateResource(User userToUpdate, Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    UserModel userModel = keycloakSession.users()
                                         .getUserById(keycloakSession.getContext().getRealm(),
                                                      userToUpdate.getId().get());
    if (userModel == null)
    {
      return null; // causes a resource not found exception you may also throw it manually
    }
    if (isChangePasswordSupported() && userToUpdate.getPassword().isPresent())
    {
      setPassword(keycloakSession, userToUpdate.getPassword().get(), userModel);
    }
    userModel = userToModel(userToUpdate, userModel);
    userModel.setSingleAttribute(AttributeNames.RFC7643.LAST_MODIFIED, String.valueOf(Instant.now().toEpochMilli()));
    User user = modelToUser(userModel);
    {
      ScimAdminEventBuilder adminEventAuditer = ((ScimKeycloakContext)context).getAdminEventAuditer();
      adminEventAuditer.createEvent(OperationType.UPDATE,
                                    ResourceType.USER,
                                    String.format("users/%s", userModel.getId()),
                                    user);
    }
    log.debug("Updated user with username: {}", userModel.getUsername());
    return user;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteResource(String id, Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    UserModel userModel = keycloakSession.users().getUserById(keycloakSession.getContext().getRealm(), id);
    if (userModel == null)
    {
      throw new ResourceNotFoundException("resource with id '" + id + "' does not exist");
    }
    keycloakSession.users().removeUser(keycloakSession.getContext().getRealm(), userModel);
    {
      ScimAdminEventBuilder adminEventAuditer = ((ScimKeycloakContext)context).getAdminEventAuditer();
      adminEventAuditer.createEvent(OperationType.DELETE,
                                    ResourceType.USER,
                                    String.format("users/%s", userModel.getId()),
                                    User.builder().id(userModel.getId()).userName(userModel.getUsername()).build());
    }
    log.debug("Deleted user with username: {}", userModel.getUsername());
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

  /**
   * writes the values of the scim user instance into the keycloak user instance
   *
   * @param user the scim user instance
   * @param userModel the keycloak user instance
   * @return the updated keycloak user instance
   */
  private UserModel userToModel(User user, UserModel userModel)
  {
    user.getExternalId()
        .ifPresent(externalId -> userModel.setSingleAttribute(AttributeNames.RFC7643.EXTERNAL_ID, externalId));
    user.isActive().ifPresent(userModel::setEnabled);
    user.getName().ifPresent(name -> {
      name.getGivenName().ifPresent(userModel::setFirstName);
      name.getFamilyName().ifPresent(userModel::setLastName);
      name.getMiddleName()
          .ifPresent(middleName -> userModel.setSingleAttribute(AttributeNames.RFC7643.MIDDLE_NAME, middleName));
      name.getHonorificPrefix()
          .ifPresent(prefix -> userModel.setSingleAttribute(AttributeNames.RFC7643.HONORIFIC_PREFIX, prefix));
      name.getHonorificSuffix()
          .ifPresent(suffix -> userModel.setSingleAttribute(AttributeNames.RFC7643.HONORIFIC_SUFFIX, suffix));
      name.getFormatted()
          .ifPresent(formatted -> userModel.setSingleAttribute(AttributeNames.RFC7643.FORMATTED, formatted));
    });
    userModel.setUsername(user.getUserName().get());
    userModel.setSingleAttribute(AttributeNames.RFC7643.NICK_NAME, user.getNickName().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.TITLE, user.getTitle().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.DISPLAY_NAME, user.getDisplayName().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.USER_TYPE, user.getUserType().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.LOCALE, user.getLocale().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.PREFERRED_LANGUAGE, user.getPreferredLanguage().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.TIMEZONE, user.getTimezone().orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.PROFILE_URL, user.getProfileUrl().orElse(null));

    user.getEmails()
        .stream()
        .filter(MultiComplexNode::isPrimary)
        .findAny()
        .flatMap(MultiComplexNode::getValue)
        .ifPresent(userModel::setEmail);

    setMultiAttribute(user::getEmails, AttributeNames.RFC7643.EMAILS, userModel);
    setMultiAttribute(user::getPhoneNumbers, AttributeNames.RFC7643.PHONE_NUMBERS, userModel);
    setMultiAttribute(user::getAddresses, AttributeNames.RFC7643.ADDRESSES, userModel);
    setMultiAttribute(user::getIms, AttributeNames.RFC7643.IMS, userModel);
    setMultiAttribute(user::getEntitlements, AttributeNames.RFC7643.ENTITLEMENTS, userModel);
    setMultiAttribute(user::getPhotos, AttributeNames.RFC7643.PHOTOS, userModel);
    setMultiAttribute(user::getRoles, AttributeNames.RFC7643.ROLES, userModel);
    setMultiAttribute(user::getX509Certificates, AttributeNames.RFC7643.X509_CERTIFICATES, userModel);

    userModel.setSingleAttribute(AttributeNames.RFC7643.COST_CENTER,
                                 user.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.DEPARTMENT,
                                 user.getEnterpriseUser().flatMap(EnterpriseUser::getDepartment).orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.DIVISION,
                                 user.getEnterpriseUser().flatMap(EnterpriseUser::getDivision).orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.EMPLOYEE_NUMBER,
                                 user.getEnterpriseUser().flatMap(EnterpriseUser::getEmployeeNumber).orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.ORGANIZATION,
                                 user.getEnterpriseUser().flatMap(EnterpriseUser::getOrganization).orElse(null));
    userModel.setSingleAttribute(AttributeNames.RFC7643.MANAGER,
                                 user.getEnterpriseUser()
                                     .flatMap(EnterpriseUser::getManager)
                                     .flatMap(Manager::getValue)
                                     .orElse(null));
    return userModel;
  }

  private void setMultiAttribute(Supplier<List<? extends MultiComplexNode>> getList,
                                 String attributeName,
                                 UserModel keycloakUser)
  {
    keycloakUser.setAttribute(attributeName,
                              getList.get()
                                     .stream()
                                     .filter(multiComplex -> !multiComplex.isPrimary())
                                     .map(MultiComplexNode::toPrettyString)
                                     .collect(Collectors.toList()));

    getList.get()
           .stream()
           .filter(MultiComplexNode::isPrimary)
           .findAny()
           .map(MultiComplexNode::toPrettyString)
           .ifPresent(multiNode -> keycloakUser.setSingleAttribute(attributeName + PRIMARY_SUFFIX, multiNode));
  }

  /**
   * converts a keycloak {@link UserModel} into a SCIM representation of {@link User}
   *
   * @param userModel the keycloak user representation
   * @return the SCIM user representation
   */
  private User modelToUser(UserModel userModel)
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

    List<GroupNode> groups = userModel.getGroupsStream().map(groupModel -> {
      return GroupNode.builder().display(groupModel.getName()).value(groupModel.getId()).type("direct").build();
    }).collect(Collectors.toList());

    User user = User.builder()
                    .id(userModel.getId())
                    .externalId(userModel.getFirstAttribute(AttributeNames.RFC7643.EXTERNAL_ID))
                    .userName(userModel.getUsername())
                    .name(name)
                    .groups(groups)
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
                    .phoneNumbers(getAttributeList(PhoneNumber.class, AttributeNames.RFC7643.PHONE_NUMBERS, userModel))
                    .addresses(getAttributeList(Address.class, AttributeNames.RFC7643.ADDRESSES, userModel))
                    .ims(getAttributeList(Ims.class, AttributeNames.RFC7643.IMS, userModel))
                    .entitlements(getAttributeList(Entitlement.class, AttributeNames.RFC7643.ENTITLEMENTS, userModel))
                    .photos(getAttributeList(Photo.class, AttributeNames.RFC7643.PHOTOS, userModel))
                    .roles(getAttributeList(PersonRole.class, AttributeNames.RFC7643.ROLES, userModel))
                    .x509Certificates(getAttributeList(ScimX509Certificate.class,
                                                       AttributeNames.RFC7643.X509_CERTIFICATES,
                                                       userModel))
                    .meta(Meta.builder()
                              .created(Optional.ofNullable(userModel.getCreatedTimestamp())
                                               .map(Instant::ofEpochMilli)
                                               .orElseGet(() -> {
                                                 log.warn("User with ID '{}' has no created timestamp",
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
    keycloakUser.getAttributeStream(attributeName + PRIMARY_SUFFIX).forEach(attribute -> {
      attributeList.add(JsonHelper.readJsonDocument(attribute, type));
    });
    return attributeList;
  }

  /**
   * gets the lastModified value of the user
   *
   * @param userModel the user model from which the last modified value should be extracted
   * @return the last modified value of the given user
   */
  private Instant getLastModified(UserModel userModel)
  {
    String lastModifiedString = userModel.getFirstAttribute(AttributeNames.RFC7643.LAST_MODIFIED);
    if (StringUtils.isNotBlank(lastModifiedString))
    {
      return Instant.ofEpochMilli(Long.parseLong(lastModifiedString));
    }
    else
    {
      return Optional.ofNullable(userModel.getCreatedTimestamp()).map(Instant::ofEpochMilli).orElse(Instant.now());
    }
  }
}
