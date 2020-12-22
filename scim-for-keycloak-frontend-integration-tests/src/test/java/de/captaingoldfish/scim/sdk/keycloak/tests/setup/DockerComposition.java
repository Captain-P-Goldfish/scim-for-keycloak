package de.captaingoldfish.scim.sdk.keycloak.tests.setup;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.testcontainers.containers.Network;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.container.DbSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.container.DockerDatabaseSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.container.DockerKeycloakSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.container.SeleniumFirefox;
import lombok.extern.slf4j.Slf4j;


/**
 * this class is used to start a new docker-setup with a keycloak and a database
 *
 * @author Pascal Knüppel
 * @since 11.12.2020
 */
@Slf4j
public class DockerComposition implements TestSetup
{

  /**
   * the docker network to use
   */
  private static final Network NETWORK = Network.builder()
                                                .driver("bridge")
                                                .createNetworkCmdModifier(cmd -> cmd.withName("keycloak-tests"))
                                                .build();

  /**
   * the docker database setup that is being used
   */
  private final DbSetup dbSetup;

  /**
   * the keycloak docker setup that is being used
   */
  private final DockerKeycloakSetup dockerKeycloakSetup;

  /**
   * a list of created web drivers that are being used to access the keycloak web admin
   */
  private List<SeleniumFirefox> seleniumFirefoxList = new ArrayList<>();

  public DockerComposition()
  {
    this.dbSetup = new DockerDatabaseSetup(NETWORK);
    this.dockerKeycloakSetup = new DockerKeycloakSetup(NETWORK, dbSetup);
  }

  /**
   * starts the docker container in the correct order
   */
  @Override
  public void start()
  {
    dbSetup.start();
    dockerKeycloakSetup.start();
    log.info("keycloak is accessible under: {}", dockerKeycloakSetup.getServerUrl());
  }

  /**
   * stops the docker container in the correct order
   */
  @Override
  public void stop()
  {
    seleniumFirefoxList.forEach(SeleniumFirefox::stop);
    dockerKeycloakSetup.stop();
    dbSetup.stop();
  }

  /**
   * @see DockerKeycloakSetup#getContainerServerUrl()
   */
  @Override
  public String getBrowserAccessUrl()
  {
    return dockerKeycloakSetup.getContainerServerUrl();
  }

  /**
   * @see DockerKeycloakSetup#getAdminUser()
   */
  @Override
  public String getAdminUserName()
  {
    return dockerKeycloakSetup.getAdminUser();
  }

  /**
   * @see DockerKeycloakSetup#getAdminPassword()
   */
  @Override
  public String getAdminUserPassword()
  {
    return dockerKeycloakSetup.getAdminPassword();
  }

  /**
   * @return the database setup
   */
  @Override
  public DbSetup getDbSetup()
  {
    return dbSetup;
  }

  /**
   * @return creates a new selenium web driver instance and starts it
   */
  @Override
  public WebDriver createNewWebDriver()
  {
    SeleniumFirefox seleniumFirefox = new SeleniumFirefox(NETWORK);
    seleniumFirefoxList.add(seleniumFirefox);
    seleniumFirefox.start();
    return seleniumFirefox.getDriver();
  }
}
