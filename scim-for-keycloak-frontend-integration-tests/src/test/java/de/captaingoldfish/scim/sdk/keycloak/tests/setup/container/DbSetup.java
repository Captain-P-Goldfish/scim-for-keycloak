package de.captaingoldfish.scim.sdk.keycloak.tests.setup.container;

import java.util.Map;


/**
 * @author Pascal Knüppel
 * @since 11.12.2020
 */
public interface DbSetup
{

  /**
   * start the database container
   */
  public void start();

  /**
   * stop the database container
   */
  public void stop();

  /**
   * @return the database url needed to access the database from the host system
   */
  public String getJdbcUrl();

  /**
   * @return the network alias to connect keycloak to the database within the docker network
   */
  public String getNetworkAlias();

  /**
   * @return the database vendor is an indicator needed by keycloak
   */
  public String getDbVendor();

  /**
   * @return the port of the database within the docker network
   */
  public String getDbPort();

  /**
   * @return the database user to access the database from the host system
   */
  public String getDbUser();

  /**
   * @return the database password to access the database from the host system
   */
  public String getDbPassword();

  /**
   * @return the database schema that should be used
   */
  public String getDbDatabase();

  /**
   * @return the connection setup to establish an {@link javax.persistence.EntityManager} connection from
   *         localhost
   */
  public Map<String, Object> getDatabaseProperties();
}
