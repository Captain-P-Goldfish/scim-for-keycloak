package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
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

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.keycloak.audit.ScimAdminEventBuilder;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
import de.captaingoldfish.scim.sdk.keycloak.provider.ScimJpaUserProvider;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimKeycloakContext;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.converter.DatabaseUserToScimConverter;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.converter.ScimUserToDatabaseConverter;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.UserFiltering;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 10.12.2022
 */
@RequiredArgsConstructor
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
    CustomUser newUser = DatabaseUserToScimConverter.databaseUserModelToScimModel(scimUserAttributes);
    {
      ScimAdminEventBuilder adminEventAuditer = ((ScimKeycloakContext)context).getAdminEventAuditer();
      adminEventAuditer.createEvent(OperationType.CREATE,
                                    ResourceType.USER,
                                    String.format("users/%s", userModel.getId()),
                                    newUser);
    }
    log.info("SCIM endpoint created user with username: {}", userModel.getUsername());
    return newUser;
  }

  /**
   * ges a single user from the database
   * 
   * @param id the id of the resource to return
   * @param attributes the attributes that should be returned to the client. If the client sends this parameter
   *          the evaluation of these parameters might help to improve database performance by omitting
   *          unnecessary table joins
   * @param excludedAttributes the attributes that should NOT be returned to the client. If the client send this
   *          parameter the evaluation of these parameters might help to improve database performance by
   *          omitting unnecessary table joins
   * @param context the current request context that holds additional useful information. This object is never
   *          null
   */
  @Override
  public CustomUser getResource(String id,
                                List<SchemaAttribute> attributes,
                                List<SchemaAttribute> excludedAttributes,
                                Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    ScimUserAttributesEntity userAttributes = ScimJpaUserProvider.findUserById(keycloakSession, id);
    if (userAttributes == null)
    {
      return null;
    }
    return DatabaseUserToScimConverter.databaseUserModelToScimModel(userAttributes);
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
                                                     .map(DatabaseUserToScimConverter::databaseUserModelToScimModel)
                                                     .collect(Collectors.toList());
    return PartialListResponse.<CustomUser> builder().totalResults(totalResults).resources(customUsers).build();
  }

  /**
   * updates an existing user within the database
   * 
   * @param userToUpdate the resource that should override an existing one
   * @param context the current request context that holds additional useful information. This object is never
   *          null
   */
  @Override
  public CustomUser updateResource(CustomUser userToUpdate, Context context)
  {
    final String id = userToUpdate.getId().orElseThrow(() -> new BadRequestException("Will never happen"));
    ScimKeycloakContext scimKeycloakContext = (ScimKeycloakContext)context;
    KeycloakSession keycloakSession = scimKeycloakContext.getKeycloakSession();
    ScimUserAttributesEntity userAttributes = ScimJpaUserProvider.findUserById(keycloakSession, id);
    if (userAttributes == null)
    {
      throw new ResourceNotFoundException(String.format("User with id '%s' does not exist", id));
    }
    userAttributes = updateUserInDatabase(userToUpdate, userAttributes, keycloakSession);
    return DatabaseUserToScimConverter.databaseUserModelToScimModel(userAttributes);
  }

  /**
   * deletes a user from the database
   * 
   * @param id the id of the resource to delete
   * @param context the current request context that holds additional useful information. This object is never
   *          null
   */
  @Override
  public void deleteResource(String id, Context context)
  {
    ScimKeycloakContext scimKeycloakContext = (ScimKeycloakContext)context;
    KeycloakSession keycloakSession = scimKeycloakContext.getKeycloakSession();
    UserModel userModel = keycloakSession.users().getUserById(keycloakSession.getContext().getRealm(), id);
    if (userModel == null)
    {
      throw new ResourceNotFoundException(String.format("User with id '%s' does not exist", id));
    }
    keycloakSession.users().removeUser(keycloakSession.getContext().getRealm(), userModel);
    {
      ScimAdminEventBuilder adminEventAuditer = ((ScimKeycloakContext)context).getAdminEventAuditer();
      adminEventAuditer.createEvent(OperationType.DELETE,
                                    ResourceType.USER,
                                    String.format("users/%s", userModel.getId()),
                                    CustomUser.builder()
                                              .id(userModel.getId())
                                              .userName(userModel.getUsername())
                                              .meta(Meta.builder()
                                                        .created(Instant.ofEpochMilli(userModel.getCreatedTimestamp()))
                                                        .lastModified(Instant.now())
                                                        .build())
                                              .build());
    }
    log.info("SCIM endpoint deleted user with username: {}", userModel.getUsername());
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
    // order is important. The addScimValuestoDatabaseModel method relies on the userEntity being added afterwards
    return setUserValuesAndSave(user, userAttributes, keycloakSession, userModel);
  }


  /**
   * updates the SCIM representation of a user within the database
   * 
   * @param user the SCIM representation of a user
   * @param userAttributes the representation of an already existing user within the database
   * @param keycloakSession the current keycloak request context
   * @return the saved database representation of the given SCIM user
   */
  private ScimUserAttributesEntity updateUserInDatabase(CustomUser user,
                                                        ScimUserAttributesEntity userAttributes,
                                                        KeycloakSession keycloakSession)
  {
    final String userName = user.getUserName().orElse(userAttributes.getUserEntity().getUsername());
    final String givenName = user.getName().flatMap(Name::getGivenName).orElse(null);
    final String familyName = user.getName().flatMap(Name::getFamilyName).orElse(null);
    final boolean userActive = user.isActive().orElse(false);
    userAttributes.getUserEntity().setUsername(userName);
    userAttributes.getUserEntity().setFirstName(givenName);
    userAttributes.getUserEntity().setLastName(familyName);
    userAttributes.getUserEntity().setEnabled(userActive);

    UserModel userModel = new UserAdapter(keycloakSession, keycloakSession.getContext().getRealm(),
                                          keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager(),
                                          userAttributes.getUserEntity());
    return setUserValuesAndSave(user, userAttributes, keycloakSession, userModel);
  }

  /**
   * adds the values from the scim representation into the database representation and saves the object
   * 
   * @param user the scim user representation
   * @param userAttributes the database user representation
   * @param keycloakSession the current request context
   * @param userModel the keycloak usermodel
   */
  @NotNull
  private ScimUserAttributesEntity setUserValuesAndSave(CustomUser user,
                                                        ScimUserAttributesEntity userAttributes,
                                                        KeycloakSession keycloakSession,
                                                        UserModel userModel)
  {
    // order is important. The addScimValuestoDatabaseModel method relies on the userEntity being added afterwards
    ScimUserToDatabaseConverter.addScimValuesToDatabaseModel(user, userModel, userAttributes);
    userAttributes.setUserEntity(((UserAdapter)userModel).getEntity());

    if (isChangePasswordSupported() && user.getPassword().isPresent())
    {
      setPassword(keycloakSession, user.getPassword().get(), userModel);
    }

    EntityManager entityManager = keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();
    log.debug("Persisting user");
    entityManager.persist(userAttributes);
    entityManager.flush();
    return userAttributes;
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
