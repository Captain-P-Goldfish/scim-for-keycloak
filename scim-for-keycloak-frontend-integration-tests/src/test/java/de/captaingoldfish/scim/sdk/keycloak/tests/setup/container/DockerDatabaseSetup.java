package de.captaingoldfish.scim.sdk.keycloak.tests.setup.container;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.utils.PropertyReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knüppel
 * @since 11.12.2020
 */
@Slf4j
@RequiredArgsConstructor
public class DockerDatabaseSetup implements DbSetup
{

  /**
   * contains the properties of the ./config/docker-setup.properties file
   */
  private static final Properties DOCKER_SETUP_PROPERTIES = PropertyReader.getDockerSetupProperties();

  /**
   * the host name to use to establish a connection to the database server
   */
  private static final String DATABASE_NETWORK_ALIAS = "database";

  /**
   * the shared network among the docker containers
   */
  private final Network network;

  /**
   * the docker container that will run the base image for the database server
   */
  @SuppressWarnings("rawtypes")
  protected GenericContainer databaseServerContainer;

  /**
   * {@inheritDoc}
   */
  @Override
  public void start()
  {
    Map<String, String> environmentVariables = getEnvironmentVariables(DOCKER_SETUP_PROPERTIES);
    String databaseImage = DOCKER_SETUP_PROPERTIES.getProperty("database.image");

    databaseServerContainer = new GenericContainer(databaseImage);
    databaseServerContainer.withNetwork(network)
                           .withNetworkAliases(DATABASE_NETWORK_ALIAS)
                           .withEnv(environmentVariables)
                           .withExposedPorts(Integer.parseInt(getDbPort()))
                           .withLogConsumer(outputFrame -> {
                             // yes! System.out.print is used on purpose here!!!!
                             System.out.print(((OutputFrame)outputFrame).getUtf8String());
                           })
                           .waitingFor(Wait.forListeningPort());

    final String hostMount = "scim-for-keycloak-frontend-tests";
    final String containerMount = DOCKER_SETUP_PROPERTIES.getProperty("database.mount");
    databaseServerContainer.setBinds(Arrays.asList(new Bind(hostMount, new Volume(containerMount))));
    databaseServerContainer.start();

    DOCKER_SETUP_PROPERTIES.setProperty("database.url",
                                        DOCKER_SETUP_PROPERTIES.getProperty("database.url")
                                                               .replace("${port}", String.valueOf(getHostPort())));
    log.warn("------------------------------------------");
    log.warn("database-url: {}", DOCKER_SETUP_PROPERTIES.getProperty("database.url"));
    log.warn("------------------------------------------");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop()
  {
    databaseServerContainer.stop();
  }

  /**
   * loads the environment variables from the docker property file
   */
  private Map<String, String> getEnvironmentVariables(Properties dockerProperties)
  {
    Map<String, String> environmentVariables = new HashMap<>();
    String environmentVariableString = dockerProperties.getProperty("database.environment");
    if (StringUtils.isNotBlank(environmentVariableString))
    {
      String[] variablePairs = environmentVariableString.split(";");
      for ( String pair : variablePairs )
      {
        String[] keyValue = pair.split("=");
        environmentVariables.put(keyValue[0], keyValue[1]);
      }
    }
    return environmentVariables;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getJdbcUrl()
  {
    return DOCKER_SETUP_PROPERTIES.getProperty("database.url");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getNetworkAlias()
  {
    return DATABASE_NETWORK_ALIAS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDbVendor()
  {
    return DOCKER_SETUP_PROPERTIES.getProperty("database.vendor");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDbPort()
  {
    return DOCKER_SETUP_PROPERTIES.getProperty("database.port");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDbUser()
  {
    return DOCKER_SETUP_PROPERTIES.getProperty("database.user");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDbPassword()
  {
    return DOCKER_SETUP_PROPERTIES.getProperty("database.password");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDbDatabase()
  {
    return DOCKER_SETUP_PROPERTIES.getProperty("database.dbDatabase");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, Object> getDatabaseProperties()
  {
    log.info("testing with {} database", getDbVendor());
    final String databaseConnectionString = getJdbcUrl() + Optional.ofNullable(getDbDatabase())
                                                                   .map(StringUtils::stripToNull)
                                                                   .map(s -> "/" + s)
                                                                   .orElse("");
    final String databaseDialect = DOCKER_SETUP_PROPERTIES.getProperty("database.dialect");
    final String databaseDriver = DOCKER_SETUP_PROPERTIES.getProperty("database.driver");

    Map<String, Object> properties = new HashMap<>();
    properties.put("javax.persistence.jdbc.driver", databaseDriver);
    properties.put("javax.persistence.jdbc.url", databaseConnectionString);
    properties.put("javax.persistence.jdbc.user", getDbUser());
    properties.put("javax.persistence.jdbc.password", getDbPassword());
    properties.put("hibernate.enable_lazy_load_no_trans", "true");

    properties.put("hibernate.dialect", databaseDialect);
    return properties;
  }

  /**
   * @return the exposed port on the host system
   */
  public int getHostPort()
  {
    return databaseServerContainer.getMappedPort(Integer.parseInt(getDbPort()));
  }
}
