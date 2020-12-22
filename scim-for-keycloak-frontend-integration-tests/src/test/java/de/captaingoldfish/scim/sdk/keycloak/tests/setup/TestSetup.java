package de.captaingoldfish.scim.sdk.keycloak.tests.setup;

import org.openqa.selenium.WebDriver;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.container.DbSetup;


/**
 * @author Pascal Knüppel
 * @since 21.08.2020
 */
public interface TestSetup
{

  /**
   * initialize the tests
   */
  public void start();

  /**
   * tear down the tests
   */
  public void stop();

  /**
   * @return the url needed by the browser to access the keycloak
   */
  public String getBrowserAccessUrl();

  /**
   * @return the name of the admin user to login to the keycloak web admin
   */
  public String getAdminUserName();

  /**
   * @return the password of the admin user to login to the keycloak web admin
   */
  public String getAdminUserPassword();

  /**
   * @return the database setup to get access to the currently running database
   */
  public DbSetup getDbSetup();

  /**
   * @return a new selenium web driver instance
   */
  public WebDriver createNewWebDriver();

}
