package de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.junit.platform.commons.util.StringUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.RoleEntity;

import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
@Slf4j
public class DirectKeycloakAccessSetup
{

  /**
   * initializes the database
   */
  private final DatabaseSetup databaseSetup;

  /**
   * creates a default configuration that we are using in our unit tests
   */
  private final KeycloakMockSetup keycloakMockSetup;

  public DirectKeycloakAccessSetup(Map<String, Object> databaseProperties)
  {
    this.databaseSetup = new DatabaseSetup(databaseProperties);
    this.keycloakMockSetup = new KeycloakMockSetup(databaseSetup.getKeycloakSession(),
                                                   databaseSetup.getEntityManager());
  }

  /**
   * the custom realm for our unit tests
   */
  public RealmModel getRealmModel()
  {
    return keycloakMockSetup.getRealmModel();
  }

  /**
   * @return the mocked keycloak session
   */
  public KeycloakSession getKeycloakSession()
  {
    return databaseSetup.getKeycloakSession();
  }

  /**
   * @return the mocked keycloak session factory
   */
  public KeycloakSessionFactory getKeycloakSessionFactory()
  {
    return keycloakMockSetup.getKeycloakSessionFactory();
  }

  /**
   * @return the entity manager that we and the keycloak tools will use to read and store entities within the
   *         database
   */
  public EntityManager getEntityManager()
  {
    return databaseSetup.getEntityManager();
  }

  /**
   * clears the current entity manager cache
   */
  public void clearCache()
  {
    getEntityManager().clear();
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
   * @return either an existing representation from the database or an empty
   */
  public ScimServiceProviderEntity getServiceProviderEntity(String realmId)
  {
    EntityManager entityManager = getEntityManager();
    ScimServiceProviderEntity scimServiceProvider;
    try
    {
      scimServiceProvider = entityManager.createNamedQuery("getScimServiceProvider", ScimServiceProviderEntity.class)
                                         .setParameter("realmId", realmId)
                                         .getSingleResult();
      return entityManager.merge(scimServiceProvider);
    }
    catch (NoResultException ex)
    {
      return null;
    }
  }

  /**
   * tries to get either all existing resource types or just the resource types of one specific realm
   *
   * @param realmId null or an existing realmId
   * @return either all resource types from the given realm or all resource types from all realms
   */
  public List<ScimResourceTypeEntity> getResourceTypeEntities(String realmId)
  {
    try
    {
      String sqlQuery = "select rt from " + ScimResourceTypeEntity.class.getSimpleName() + " rt";
      if (StringUtils.isNotBlank(realmId))
      {
        sqlQuery += " where rt.realmId = :realmId";
      }
      Query query = getEntityManager().createQuery(sqlQuery, ScimResourceTypeEntity.class);
      if (StringUtils.isNotBlank(realmId))
      {
        query.setParameter("realmId", realmId);
      }
      return query.getResultList();

    }
    catch (NoResultException ex)
    {
      return new ArrayList<>();
    }
  }

  /**
   * the clients of the given realm
   * 
   * @param realmId the realm of which the clients should be returned
   */
  public List<ClientEntity> getAllClientsOfRealm(String realmId)
  {
    try
    {
      String sqlQuery = "select client from " + ClientEntity.class.getSimpleName() + " client";
      if (StringUtils.isNotBlank(realmId))
      {
        sqlQuery += " where client.realmId = :realmId";
      }
      TypedQuery<ClientEntity> query = getEntityManager().createQuery(sqlQuery, ClientEntity.class);
      if (StringUtils.isNotBlank(realmId))
      {
        query.setParameter("realmId", realmId);
      }

      return query.getResultList();

    }
    catch (NoResultException ex)
    {
      return new ArrayList<>();
    }
  }

  /**
   * the realm roles of the given realm
   * 
   * @param realmId the realm of which the clients should be returned
   */
  public List<RoleEntity> getAllRealmRolesOfRealm(String realmId)
  {
    try
    {
      String sqlQuery = "select role from " + RoleEntity.class.getSimpleName() + " role where role.clientRole is false";
      if (StringUtils.isNotBlank(realmId))
      {
        sqlQuery += " and role.realmId = :realmId";
      }
      TypedQuery<RoleEntity> query = getEntityManager().createQuery(sqlQuery, RoleEntity.class);
      if (StringUtils.isNotBlank(realmId))
      {
        query.setParameter("realmId", realmId);
      }

      return query.getResultList();

    }
    catch (NoResultException ex)
    {
      return new ArrayList<>();
    }
  }
}
