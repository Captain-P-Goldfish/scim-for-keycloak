package de.captaingoldfish.scim.sdk.keycloak.audit;

import java.util.Optional;

import javax.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.ClientAdapter;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 09.07.2021
 */
@Slf4j
public class ScimAdminEventBuilder
{

  /**
   * the current keycloak request context
   */
  private final KeycloakSession keycloakSession;

  /**
   * the admin event builder to create admin events in the database
   */
  private final AdminEventBuilder adminEventBuilder;

  public ScimAdminEventBuilder(KeycloakSession keycloakSession, AdminAuth adminAuth)
  {
    this.keycloakSession = keycloakSession;
    KeycloakContext context = keycloakSession.getContext();
    RealmModel realm = context.getRealm();
    adminEventBuilder = new AdminEventBuilder(realm,
                                              Optional.ofNullable(adminAuth).orElseGet(this::getAnonymousAdminAuth),
                                              keycloakSession, context.getConnection());
  }

  /**
   * creates an anonymous authentication object used in cases when authentication is disabled
   */
  private AdminAuth getAnonymousAdminAuth()
  {
    EntityManager entityManager = keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();
    UserEntity userEntity = new UserEntity();
    userEntity.setId("anonymous");
    userEntity.setUsername("anonymous");
    UserModel user = new UserAdapter(keycloakSession, keycloakSession.getContext().getRealm(), entityManager,
                                     userEntity);

    ClientEntity clientEntity = new ClientEntity();
    clientEntity.setId("anonymous");
    clientEntity.setClientId("anonymous");
    ClientModel client = new ClientAdapter(keycloakSession.getContext().getRealm(), entityManager, keycloakSession,
                                           clientEntity);

    AccessToken accessToken = new AccessToken();
    return new AdminAuth(keycloakSession.getContext().getRealm(), accessToken, user, client);
  }

  /**
   * creates an admin event
   */
  public void createEvent(OperationType operationType,
                          ResourceType resourceType,
                          String resourcePath,
                          Object representation)
  {
    log.trace("creating admin event '{}' '{}' '{}' '{}'", resourceType, operationType, resourcePath, representation);
    adminEventBuilder.operation(operationType)
                     .resource(resourceType)
                     .realm(keycloakSession.getContext().getRealm())
                     .resourcePath(resourcePath)
                     .representation(representation)
                     .refreshRealmEventsConfig(keycloakSession)
                     .success();
  }

}
