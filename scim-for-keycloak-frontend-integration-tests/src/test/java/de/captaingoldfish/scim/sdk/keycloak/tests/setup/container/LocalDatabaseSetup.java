package de.captaingoldfish.scim.sdk.keycloak.tests.setup.container;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.utils.PropertyReader;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
@Slf4j
public class LocalDatabaseSetup implements DbSetup
{

  /**
   * contains the properties of the ./config/local-setup.properties file
   */
  private static final Properties LOCAL_SETUP_PROPERTIES = PropertyReader.getLocalSetupProperties();

  /**
   * {@inheritDoc}
   */
  @Override
  public void start()
  {
    // do nothing
  }

  /**
   * not needed
   */
  @Override
  public void stop()
  {
    // do nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getJdbcUrl()
  {
    return LOCAL_SETUP_PROPERTIES.getProperty("database.url");
  }

  /**
   * not needed
   */
  @Override
  public String getNetworkAlias()
  {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDbVendor()
  {
    return LOCAL_SETUP_PROPERTIES.getProperty("database.vendor");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDbPort()
  {
    return LOCAL_SETUP_PROPERTIES.getProperty("database.port");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDbUser()
  {
    return LOCAL_SETUP_PROPERTIES.getProperty("database.user");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDbPassword()
  {
    return LOCAL_SETUP_PROPERTIES.getProperty("database.password");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDbDatabase()
  {
    return LOCAL_SETUP_PROPERTIES.getProperty("database.dbDatabase");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, Object> getDatabaseProperties()
  {
    log.info("testing with {} database", getDbVendor());
    final String databaseConnectionString = getJdbcUrl() + "/" + getDbDatabase();
    final String databaseDialect = LOCAL_SETUP_PROPERTIES.getProperty("database.dialect");
    final String databaseDriver = LOCAL_SETUP_PROPERTIES.getProperty("database.driver");

    Map<String, Object> properties = new HashMap<>();
    properties.put("javax.persistence.jdbc.driver", databaseDriver);
    properties.put("javax.persistence.jdbc.url", databaseConnectionString);
    properties.put("javax.persistence.jdbc.user", getDbUser());
    properties.put("javax.persistence.jdbc.password", getDbPassword());
    properties.put("hibernate.enable_lazy_load_no_trans", "true");

    properties.put("hibernate.dialect", databaseDialect);
    return properties;
  }
}
