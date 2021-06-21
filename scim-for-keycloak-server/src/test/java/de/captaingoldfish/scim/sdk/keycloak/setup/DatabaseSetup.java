package de.captaingoldfish.scim.sdk.keycloak.setup;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.h2.tools.Server;
import org.hibernate.internal.SessionImpl;
import org.keycloak.connections.jpa.DefaultJpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.updater.liquibase.ThreadLocalSessionContext;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.JpaRealmProvider;
import org.keycloak.models.jpa.JpaUserProvider;
import org.keycloak.storage.ClientStorageManager;
import org.keycloak.storage.UserStorageManager;
import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.keycloak.provider.ScimEndpointProviderFactory;
import de.captaingoldfish.scim.sdk.keycloak.provider.ScimJpaEntityProvider;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 05.08.2020
 */
@Slf4j
class DatabaseSetup
{

  /**
   * to create a new {@link EntityManager}
   */
  // @formatter:off
  private static final EntityManagerFactory ENTITY_MANAGER_FACTORY =
    Persistence.createEntityManagerFactory("keycloak-default", getH2DatabaseProperties());
  // @formatter:on

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

  public DatabaseSetup()
  {
    this.keycloakSession = Mockito.mock(KeycloakSession.class);
    this.entityManager = buildEntityManager();
    buildDatabase();
  }

  /**
   * this method will build a connection setup for a h2 database
   */
  private static Map<String, Object> getH2DatabaseProperties()
  {
    log.info("testing with h2 database");
    final String databaseUser = "sa";
    final String databasePassword = "";
    // final String DATABASE_CONNECTION_STRING = "jdbc:h2:file:./target/junit-database/keycloak-testdb";
    final String databaseConnectionString = "jdbc:h2:mem:keycloak";
    final String databaseDialect = "org.hibernate.dialect.H2Dialect";
    final String databaseDriver = "org.h2.Driver";

    Map<String, Object> properties = new HashMap<>();
    properties.put("javax.persistence.jdbc.driver", databaseDriver);
    properties.put("javax.persistence.jdbc.url", databaseConnectionString);
    properties.put("javax.persistence.jdbc.user", databaseUser);
    properties.put("javax.persistence.jdbc.password", databasePassword);
    properties.put("hibernate.dialect", databaseDialect);
    addAdditionalEntities(properties);

    try
    {
      Server server = Server.createTcpServer().start();
      log.info("Server started and connection is open.");
      log.info("URL: jdbc:h2:" + server.getURL() + "/mem:mem:testdb");
    }
    catch (SQLException e)
    {
      throw new RuntimeException(e);
    }

    return properties;
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
    EntityManager newEntityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
    JpaUserProvider jpaUserProvider = new JpaUserProvider(keycloakSession, newEntityManager);
    JpaRealmProvider jpaRealmProvider = new JpaRealmProvider(keycloakSession, newEntityManager, Collections.emptySet());
    Mockito.doReturn(new UserStorageManager(keycloakSession)).when(keycloakSession).users();
    Mockito.doReturn(jpaUserProvider).when(keycloakSession).userLocalStorage();
    Mockito.doReturn(jpaRealmProvider).when(keycloakSession).realms();
    Mockito.doReturn(jpaRealmProvider).when(keycloakSession).realmLocalStorage();
    ClientProvider clientProvider = Mockito.spy(new ClientStorageManager(keycloakSession, 10000));
    Mockito.doReturn(jpaRealmProvider).when(keycloakSession).clients();
    Mockito.doReturn(clientProvider).when(keycloakSession).clientLocalStorage();
    Mockito.doReturn(new DefaultJpaConnectionProvider(newEntityManager))
           .when(keycloakSession)
           .getProvider(JpaConnectionProvider.class);
    return newEntityManager;
  }

  /**
   * setup database configuration
   */
  private final void buildDatabase()
  {
    // keycloak did implement a custom liquibase module that is used when executing the changesets. This
    // custom module needs the keycloakSession which it will retrieve from the ThreadLocalSessionContext
    ThreadLocalSessionContext.setCurrentSession(keycloakSession);

    log.trace("building database with liquibase");
    try
    {
      JdbcConnection jdbcConnection = new JdbcConnection(entityManager.unwrap(SessionImpl.class).connection());
      Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);
      ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(ScimEndpointProviderFactory.class.getClassLoader());
      final Liquibase liquibaseKeycloak = new Liquibase("META-INF/jpa-changelog-master.xml", resourceAccessor, db);
      liquibaseKeycloak.update(new Contexts((String)null), new LabelExpression((String)null));
      final Liquibase liquibaseScimSdk = new Liquibase("META-INF/scim-changelog.xml", resourceAccessor, db);
      liquibaseScimSdk.update(new Contexts((String)null), new LabelExpression((String)null));
      log.debug("liquibase executed successfully");
    }
    catch (LiquibaseException e)
    {
      throw new RuntimeException("Error during liquibase execution", e); // NOPMD
    }
  }
}
