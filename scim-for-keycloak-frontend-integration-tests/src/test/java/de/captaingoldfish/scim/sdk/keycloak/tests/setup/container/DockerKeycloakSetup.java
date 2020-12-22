package de.captaingoldfish.scim.sdk.keycloak.tests.setup.container;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.utils.PropertyReader;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knüppel
 * @since 11.12.2020
 */
@Slf4j
public class DockerKeycloakSetup
{

  /**
   * contains the properties of the ./config/docker-setup.properties file
   */
  private static final Properties DOCKER_PROPERTIES = PropertyReader.getDockerSetupProperties();

  /**
   * the docker keycloak image that should be used
   */
  private static final String KEYCLOAK_IMAGE = DOCKER_PROPERTIES.getProperty("keycloak.image");

  /**
   * the host name to use to establish a connection to the keycloak server
   */
  protected final String keycloakNetworkAlias = "keycloak";

  /**
   * the port keycloak uses within the container
   */
  protected final int keycloakPort = 8080;

  /**
   * the shared network among the docker containers
   */
  private final Network network;

  /**
   * the database setup that is needed to configure keycloak to access the database
   */
  private final DbSetup dbSetup;

  /**
   * the docker container that will run the base image for the ms sql server
   */
  @SuppressWarnings("rawtypes")
  protected GenericContainer keycloakContainer;

  public DockerKeycloakSetup(Network network, DbSetup dbSetup)
  {
    this.network = network;
    this.dbSetup = dbSetup;
  }

  /**
   * start the keycloak docker container
   */
  public void start()
  {
    Map<String, String> environmentVariables = new HashMap<>();
    environmentVariables.put("KEYCLOAK_USER", getAdminUser());
    environmentVariables.put("KEYCLOAK_PASSWORD", getAdminPassword());
    environmentVariables.put("DB_VENDOR", dbSetup.getDbVendor());
    environmentVariables.put("DB_ADDR", dbSetup.getNetworkAlias());
    environmentVariables.put("DB_PORT", dbSetup.getDbPort());
    environmentVariables.put("DB_USER", dbSetup.getDbUser());
    environmentVariables.put("DB_PASSWORD", dbSetup.getDbPassword());
    environmentVariables.put("DB_DATABASE", dbSetup.getDbDatabase());

    final String keycloakDeployment = "/opt/jboss/keycloak/standalone/deployments/scim-for-keycloak.ear";
    keycloakContainer = new GenericContainer(KEYCLOAK_IMAGE);
    keycloakContainer.withNetwork(network)
                     .withNetworkAliases(keycloakNetworkAlias)
                     .withExposedPorts(keycloakPort)
                     .withEnv(environmentVariables)
                     .withFileSystemBind(getDeploymentPath(), keycloakDeployment)
                     .withLogConsumer(outputFrame -> {
                       // yes! System.out.print is used on purpose here!!!!
                       System.out.print(((OutputFrame)outputFrame).getUtf8String());
                     })
                     .waitingFor(new HttpWaitStrategy().forStatusCode(HttpStatus.OK)
                                                       .forPath("/auth/admin")
                                                       .forPort(8080)
                                                       .withStartupTimeout(Duration.ofMinutes(3)))
                     .withStartupTimeout(Duration.ofMinutes(3));
    keycloakContainer.start();
    log.warn("------------------------------------------");
    log.warn("keycloak-port: {}", getHostPort());
    log.warn("------------------------------------------");
  }

  /**
   * @return the path for the scim-for-keycloak deployment file
   */
  private String getDeploymentPath()
  {
    final String currentVersion = PropertyReader.getCurrentVersion();
    final String deploymentFilename = "scim-for-keycloak-" + currentVersion + ".ear";
    File deploymentFile = new File("../scim-for-keycloak-deployment/target/" + deploymentFilename);
    if (!deploymentFile.exists())
    {
      throw new IllegalStateException("could not find deployment file '" + deploymentFile.getAbsolutePath() + "' "
                                      + "please execute 'mvn clean package' on the parent directory of the project first");
    }
    return deploymentFile.getAbsolutePath();
  }

  /**
   * stop the keycloak server
   */
  public void stop()
  {
    if (keycloakContainer != null)
    {
      keycloakContainer.stop();
    }
  }

  /**
   * @return the admin user of the keycloak
   */
  public String getAdminUser()
  {
    return "admin";
  }

  /**
   * @return the admin password of the keycloak
   */
  public String getAdminPassword()
  {
    return "admin";
  }

  /**
   * @return the host port to access the keycloak from localhost
   */
  public int getHostPort()
  {
    return keycloakContainer.getMappedPort(keycloakPort);
  }

  /**
   * @return the url to access the keycloak from within the docker network
   */
  public String getContainerServerUrl()
  {
    return "http://" + keycloakNetworkAlias + ":" + keycloakPort;
  }

  /**
   * @return the server url to access the keycloak from the host system
   */
  public String getServerUrl()
  {
    return "http://localhost:" + getHostPort();
  }

}
