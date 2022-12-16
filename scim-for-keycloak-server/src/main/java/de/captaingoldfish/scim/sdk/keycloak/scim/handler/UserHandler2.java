package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

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

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.keycloak.audit.ScimAdminEventBuilder;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
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
    log.debug("Created user with username: {}", userModel.getUsername());
    return newUser;
  }

  @Override
  public CustomUser getResource(String id,
                                List<SchemaAttribute> attributes,
                                List<SchemaAttribute> excludedAttributes,
                                Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    EntityManager entityManager = keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();
    final String queryName = ScimUserAttributesEntity.GET_SCIM_USER_ATTRIBUTES_QUERY_NAME;
    TypedQuery<ScimUserAttributesEntity> query = entityManager.createNamedQuery(queryName,
                                                                                ScimUserAttributesEntity.class);
    query.setParameter("userId", id);
    ScimUserAttributesEntity userAttributes;
    try
    {
      userAttributes = query.getSingleResult();
    }
    catch (NoResultException ex)
    {
      log.debug(ex.getMessage());
      // causes a 404 not found exception
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
                                                     .map(DatabaseUserToScimConverter::databaseUserModelToScimModel)
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
    // order is important. The addScimValuestoDatabaseModel method relies on the userEntity being added afterwards
    ScimUserToDatabaseConverter.addScimValuesToDatabaseModel(user, userModel, userAttributes);
    userAttributes.setUserEntity(((UserAdapter)userModel).getEntity());

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
