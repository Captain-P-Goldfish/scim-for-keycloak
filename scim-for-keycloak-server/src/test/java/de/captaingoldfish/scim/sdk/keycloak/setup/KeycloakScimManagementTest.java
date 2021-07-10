package de.captaingoldfish.scim.sdk.keycloak.setup;

import java.math.BigInteger;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.jpa.AdminEventEntity;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.ClientScopeAttributeEntity;
import org.keycloak.models.jpa.entities.ClientScopeClientMappingEntity;
import org.keycloak.models.jpa.entities.ClientScopeEntity;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.jpa.entities.GroupAttributeEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.GroupRoleMappingEntity;
import org.keycloak.models.jpa.entities.ProtocolMapperEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RoleAttributeEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;
import org.keycloak.models.jpa.entities.UserRoleMappingEntity;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.resources.admin.AdminAuth;
import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.keycloak.auth.Authentication;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimConfigurationBridge;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimEndpoint;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 05.08.2020
 */
@Slf4j
public abstract class KeycloakScimManagementTest
{

  /**
   * initializes the database
   */
  private static final DatabaseSetup DATABASE_SETUP = new DatabaseSetup();

  /**
   * creates a default configuration that we are using in our unit tests
   */
  private static final KeycloakMockSetup KEYCLOAK_MOCK_SETUP = new KeycloakMockSetup(DATABASE_SETUP.getKeycloakSession(),
                                                                                     DATABASE_SETUP.getEntityManager());

  /**
   * the custom endpoint that is under test
   */
  @Getter
  @Setter
  private ScimEndpoint scimEndpoint;

  /**
   * the mocked authentication object that is used by the {@link #scimEndpoint}
   */
  @Getter
  private Authentication authentication;

  /**
   * the custom realm for our unit tests
   */
  public RealmModel getRealmModel()
  {
    return KEYCLOAK_MOCK_SETUP.getRealmModel();
  }

  /**
   * @return a test client
   */
  public ClientModel getTestClient()
  {
    return KEYCLOAK_MOCK_SETUP.getClient();
  }

  /**
   * @return a test user
   */
  public UserModel getTestUser()
  {
    return KEYCLOAK_MOCK_SETUP.getUser();
  }

  /**
   * the mocked keycloak session
   */
  public KeycloakSession getKeycloakSession()
  {
    return DATABASE_SETUP.getKeycloakSession();
  }

  public KeycloakSessionFactory getKeycloakSessionFactory()
  {
    return KEYCLOAK_MOCK_SETUP.getKeycloakSessionFactory();
  }

  /**
   * the entitymanager that we and the keycloak tools will use to read and store entities within the database
   */
  public EntityManager getEntityManager()
  {
    return DATABASE_SETUP.getEntityManager();
  }

  /**
   * persists an entity and flushes the current transaction
   */
  public void persist(Object entity)
  {
    getEntityManager().persist(entity);
    getEntityManager().flush();
  }

  /**
   * begin a transaction before each test
   */
  @BeforeEach
  public void initializeKeycloakSetup()
  {
    clearTables();
    KEYCLOAK_MOCK_SETUP.createRealm();
    beginTransaction();
    initializeEndpoint();
  }

  /**
   * initializes the endpoint under test
   */
  public void initializeEndpoint()
  {
    this.authentication = Mockito.spy(new Authentication());
    AdminAuth adminAuth = mockUnitTestAuthentication();
    Mockito.doReturn(adminAuth).when(authentication).authenticate(getKeycloakSession());
    Mockito.doNothing().when(authentication).authenticateAsScimAdmin(getKeycloakSession());

    // before creation the tables must be empty
    Assertions.assertEquals(0, countEntriesInTable(ScimServiceProviderEntity.class));
    Assertions.assertEquals(0, countEntriesInTable(ScimResourceTypeEntity.class));
    scimEndpoint = Mockito.spy(new ScimEndpoint(getKeycloakSession(), authentication));
    // after creation the endpoint must create the service provider configuration
    Assertions.assertEquals(1, countEntriesInTable(ScimServiceProviderEntity.class));
    // the database entries for the resource types User and Group must be directly initialized
    Assertions.assertEquals(3, countEntriesInTable(ScimResourceTypeEntity.class));
  }

  /**
   * mocks the unit test authentication object
   */
  public AdminAuth mockUnitTestAuthentication()
  {
    AdminAuth adminAuth = Mockito.mock(AdminAuth.class);
    Mockito.doReturn(getRealmModel()).when(adminAuth).getRealm();
    Mockito.doReturn(getTestClient()).when(adminAuth).getClient();
    Mockito.doReturn(getTestUser()).when(adminAuth).getUser();
    AccessToken token = Mockito.mock(AccessToken.class);
    Mockito.doReturn(token).when(adminAuth).getToken();
    return adminAuth;
  }

  /**
   * will destroy the current test-setup by deleting the created entities again for example
   */
  @AfterEach
  public void destroy()
  {
    clearTables();
    getEntityManager().clear(); // clears the cache of the entityManager etc.
    commitTransaction();
    ScimConfigurationBridge.clearScimContext();
  }

  /**
   * used to start a new JPA transaction
   */
  public void beginTransaction()
  {
    if (!getEntityManager().getTransaction().isActive())
    {
      getEntityManager().getTransaction().begin();
    }
  }

  /**
   * commits a transaction if any is active
   */
  public void commitTransaction()
  {
    if (getEntityManager().getTransaction().isActive())
    {
      getEntityManager().getTransaction().commit();
    }
  }

  /**
   * does a rollback on the transaction in cases that an exception has occured due to a unique constraint
   * failure for example
   */
  public void rollbackTransaction()
  {
    if (getEntityManager().getTransaction().isActive())
    {
      getEntityManager().getTransaction().rollback();
    }
  }

  /**
   * this method is used as a clean up operations on the database tables that are used on SCIM testing
   */
  public void clearTables()
  {
    log.debug("cleaning up tables");

    deleteFromMappingTable("CLIENT_ATTRIBUTES");
    deleteFromMappingTable("REDIRECT_URIS");
    deleteFromMappingTable("SCOPE_MAPPING");
    deleteFromMappingTable("PROTOCOL_MAPPER_CONFIG");

    deleteFromTable(ScimServiceProviderEntity.class);
    deleteFromTable(ScimResourceTypeEntity.class);
    deleteFromTable(UserGroupMembershipEntity.class);
    deleteFromTable(UserRoleMappingEntity.class);
    deleteFromTable(GroupRoleMappingEntity.class);
    deleteFromTable(UserAttributeEntity.class);
    deleteFromTable(CredentialEntity.class);
    deleteFromTable(UserEntity.class);
    deleteFromTable(GroupAttributeEntity.class);
    deleteFromTable(GroupEntity.class);
    deleteFromTable(ProtocolMapperEntity.class);
    deleteFromTable(ClientScopeClientMappingEntity.class);
    deleteFromTable(ClientScopeAttributeEntity.class);
    deleteFromTable(ClientScopeEntity.class);
    deleteFromTable(RoleAttributeEntity.class);
    deleteFromTable(RoleEntity.class);
    deleteFromTable(ClientEntity.class);
    deleteFromTable(RealmEntity.class);
    deleteFromTable(AdminEventEntity.class);
    log.debug("cleaned tables successfully");
  }

  /**
   * will delete the entries of a single table
   *
   * @param tableName the name of the table that should be deleted
   */
  public void deleteFromMappingTable(String tableName)
  {
    beginTransaction();
    getEntityManager().createNativeQuery("delete from " + tableName).executeUpdate();
    commitTransaction();
  }

  /**
   * will delete the entries of a single table
   *
   * @param entityClass the entity whose entries should be deleted
   */
  public void deleteFromTable(Class<?> entityClass)
  {
    beginTransaction();
    getEntityManager().createQuery("delete from " + entityClass.getSimpleName()).executeUpdate();
    commitTransaction();
  }

  /**
   * counts the number of entries within the given table
   *
   * @param entityClass the class-type of the entity whose entries should be counted
   * @return the number of entries within the database of the given entity-type
   */
  public int countEntriesInTable(Class<?> entityClass)
  {
    return ((Long)getEntityManager().createQuery("select count(entity) from " + entityClass.getSimpleName() + " entity")
                                    .getSingleResult()).intValue();
  }

  /**
   * counts the number of entries within the given table
   *
   * @param tableName the name of the table from which the entries should be counted
   * @return the number of entries within the database of the given entity-type
   */
  public int countEntriesInMappingTable(String tableName)
  {
    return ((BigInteger)getEntityManager().createNativeQuery("select count(*) from " + tableName)
                                          .getSingleResult()).intValue();
  }

  /**
   * @return an admin event store provider that is used to check the current queries within the database
   */
  public EventStoreProvider getAdminEventStoreProvider()
  {
    return KEYCLOAK_MOCK_SETUP.getEventStoreProvider();
  }
}
