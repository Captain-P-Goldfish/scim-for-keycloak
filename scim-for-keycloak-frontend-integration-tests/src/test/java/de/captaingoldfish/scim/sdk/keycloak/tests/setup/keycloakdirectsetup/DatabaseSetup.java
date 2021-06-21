package de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup;

import java.util.Collections;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.keycloak.connections.jpa.DefaultJpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.JpaRealmProvider;
import org.keycloak.models.jpa.JpaUserProvider;
import org.keycloak.storage.ClientStorageManager;
import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.keycloak.provider.ScimJpaEntityProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * creates
 *
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
@Slf4j
class DatabaseSetup
{

  private final EntityManagerFactory entityManagerFactory;

  /**
   * the entitymanager that we and the keycloak tools will use to read and store entities within the database
   */
  @Getter
  private final EntityManager entityManager;

  /**
   * the mocked keycloak session
   */
  @Getter
  private KeycloakSession keycloakSession;

  public DatabaseSetup(Map<String, Object> databaseProperties)
  {
    addAdditionalEntities(databaseProperties);
    this.entityManagerFactory = Persistence.createEntityManagerFactory("keycloak-default", databaseProperties);
    this.keycloakSession = Mockito.mock(KeycloakSession.class);
    this.entityManager = buildEntityManager();
  }

  /**
   * this method will extend the database properties by the custom entities
   */
  private static void addAdditionalEntities(Map<String, Object> properties)
  {
    properties.put(org.hibernate.jpa.AvailableSettings.LOADED_CLASSES, new ScimJpaEntityProvider().getEntities());
  }

  /**
   * creates the entity manager that keycloak and we will work with
   *
   * @return the entity manager on the configured database
   */
  private EntityManager buildEntityManager()
  {
    EntityManager newEntityManager = entityManagerFactory.createEntityManager();
    JpaUserProvider jpaUserProvider = new JpaUserProvider(keycloakSession, newEntityManager);
    JpaRealmProvider jpaRealmProvider = new JpaRealmProvider(keycloakSession, newEntityManager, Collections.emptySet());
    Mockito.doReturn(new JpaUserProvider(keycloakSession, newEntityManager)).when(keycloakSession).users();
    Mockito.doReturn(jpaUserProvider).when(keycloakSession).userLocalStorage();
    Mockito.doReturn(jpaRealmProvider).when(keycloakSession).realms();
    Mockito.doReturn(jpaRealmProvider).when(keycloakSession).realmLocalStorage();
    ClientProvider clientProvider = Mockito.spy(new ClientStorageManager(keycloakSession, 10000));
    Mockito.doReturn(jpaRealmProvider).when(keycloakSession).clientStorageManager();
    Mockito.doReturn(clientProvider).when(keycloakSession).clientLocalStorage();
    Mockito.doReturn(new DefaultJpaConnectionProvider(newEntityManager))
           .when(keycloakSession)
           .getProvider(JpaConnectionProvider.class);
    return newEntityManager;
  }
}
