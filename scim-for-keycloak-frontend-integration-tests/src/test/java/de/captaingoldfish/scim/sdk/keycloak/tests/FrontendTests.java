package de.captaingoldfish.scim.sdk.keycloak.tests;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.TestSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup.DirectKeycloakAccessSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder.ActivateScimThemeTestBuilder;
import de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder.CreateNewRealmTestBuilder;
import de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder.RealmTestBuilder;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
@Slf4j
public abstract class FrontendTests
{

  /**
   * the test setup that should be executed. It represents either a docker setup or a already running local
   * setup
   */
  private final TestSetup testSetup;

  /**
   * creates a direct connection to the currently running database with a mocked keycloak session setup
   */
  private DirectKeycloakAccessSetup directKeycloakAccessSetup;

  public FrontendTests(TestSetup testSetup)
  {
    this.testSetup = testSetup;
  }

  /**
   * initializes the test setup
   */
  @BeforeEach
  public void initializeSetup()
  {
    testSetup.start();
    this.directKeycloakAccessSetup = new DirectKeycloakAccessSetup(testSetup.getDbSetup().getDatabaseProperties());
  }

  /**
   * tears down the test setup
   */
  @AfterEach
  public void tearDownSetup()
  {
    testSetup.stop();
  }

  /**
   * defines the frontend tests for the scim-for-keycloak application
   */
  @Tag("integration-tests")
  @TestFactory
  public List<DynamicTest> testScimForKeycloakFrontend()
  {
    WebDriver webDriver = testSetup.createNewWebDriver();

    loginOnAdminConsole(testSetup, webDriver);

    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.addAll(new ActivateScimThemeTestBuilder(webDriver, testSetup,
                                                         directKeycloakAccessSetup).buildDynamicTests());
    dynamicTests.addAll(new RealmTestBuilder(webDriver, testSetup, directKeycloakAccessSetup,
                                             "master").buildDynamicTests());
    dynamicTests.addAll(new CreateNewRealmTestBuilder(webDriver, testSetup,
                                                      directKeycloakAccessSetup).buildDynamicTests());
    return dynamicTests;
  }

  /**
   * executes the login on the keycloak web admin console
   */
  private void loginOnAdminConsole(TestSetup testSetup, WebDriver webDriver)
  {
    final WebDriverWait wait = new WebDriverWait(webDriver, 5000);
    final String loginAddress = testSetup.getBrowserAccessUrl() + "/auth/admin/";
    webDriver.get(loginAddress);

    WebElement usernameInput = wait.until(d -> d.findElement(By.id("username")));
    WebElement passwordInput = webDriver.findElement(By.id("password"));
    WebElement loginForm = webDriver.findElement(By.id("kc-login"));

    usernameInput.sendKeys(testSetup.getAdminUserName());
    passwordInput.sendKeys(testSetup.getAdminUserPassword());
    loginForm.click();
    // keycloak is acting differently based on the case that the server is running on windows or in a docker
    // environment. Therefore we are calling the master realm directly to ensure that we start on the master
    // realms overview page
    final String masterRealmUrl = loginAddress + "master/console/#/realms/master";
    webDriver.get(masterRealmUrl);
  }
}
