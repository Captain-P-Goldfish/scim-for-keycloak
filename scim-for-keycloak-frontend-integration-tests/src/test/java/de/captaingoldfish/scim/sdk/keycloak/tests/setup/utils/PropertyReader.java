package de.captaingoldfish.scim.sdk.keycloak.tests.setup.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import lombok.SneakyThrows;


/**
 * this class is used to read the property setup from the /config directory
 * 
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
public class PropertyReader
{

  /**
   * the properties for a setup that is being started in a docker environment with testcontainers
   */
  private static Properties DOCKER_PROPERTIES;

  /**
   * the properties for a already running local setup
   */
  private static Properties LOCAL_PROPERTIES;

  /**
   * the current project version
   */
  private static String CURRENT_VERSION;

  /**
   * reads the properties for a complete docker setup that is being started and used to run the integration
   * tests
   */
  @SneakyThrows
  public static Properties getDockerSetupProperties()
  {
    if (DOCKER_PROPERTIES != null)
    {
      return DOCKER_PROPERTIES;
    }
    DOCKER_PROPERTIES = new Properties();
    File dockerPropertiesFile = new File(PropertyReader.class.getResource("/").getFile()
                                         + "/../../config/docker-setup.properties");
    try (InputStream inputStream = new FileInputStream(dockerPropertiesFile))
    {
      DOCKER_PROPERTIES.load(inputStream);
    }
    return DOCKER_PROPERTIES;
  }

  /**
   * reads the properties for a local db-keycloak custom setup that is used to run the integration tests
   */
  @SneakyThrows
  public static Properties getLocalSetupProperties()
  {
    if (LOCAL_PROPERTIES != null)
    {
      return LOCAL_PROPERTIES;
    }
    LOCAL_PROPERTIES = new Properties();
    File localPropertiesFile = new File(PropertyReader.class.getResource("/").getFile()
                                        + "/../../config/local-setup.properties");
    try (InputStream inputStream = new FileInputStream(localPropertiesFile))
    {
      LOCAL_PROPERTIES.load(inputStream);
    }
    return LOCAL_PROPERTIES;
  }

  /**
   * @return the current maven version of this project
   */
  @SneakyThrows
  public static String getCurrentVersion()
  {
    if (CURRENT_VERSION != null)
    {
      return CURRENT_VERSION;
    }
    Properties mavenProperties = new Properties();
    try (InputStream inputStream = PropertyReader.class.getResourceAsStream("/maven.properties"))
    {
      mavenProperties.load(inputStream);
    }
    CURRENT_VERSION = mavenProperties.getProperty("project.version");
    return CURRENT_VERSION;
  }

  /**
   * checks the property files are read correctly
   */
  @Test
  public void loadSetupProperties()
  {
    Assertions.assertDoesNotThrow(PropertyReader::getDockerSetupProperties);
    Assertions.assertDoesNotThrow(PropertyReader::getLocalSetupProperties);
  }
}
