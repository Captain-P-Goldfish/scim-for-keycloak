package de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.TestSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup.DirectKeycloakAccessSetup;


/**
 * @author Pascal Knueppel
 * @since 21.12.2020
 */
public class RealmTestBuilder extends AbstractTestBuilder
{

  private final String currentRealm;

  public RealmTestBuilder(WebDriver webDriver,
                          TestSetup testSetup,
                          DirectKeycloakAccessSetup directKeycloakAccessSetup,
                          String realm)
  {
    super(webDriver, testSetup, directKeycloakAccessSetup);
    this.currentRealm = realm;
  }

  @Override
  public List<DynamicTest> buildDynamicTests()
  {
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(enableScim(true));
    ServiceProviderConfigTestBuilder serviceProviderTestBuilder = new ServiceProviderConfigTestBuilder(webDriver,
                                                                                                       testSetup,
                                                                                                       directKeycloakAccessSetup,
                                                                                                       currentRealm);
    dynamicTests.addAll(serviceProviderTestBuilder.buildDynamicTests());
    dynamicTests.addAll(new ServiceProviderAuthorizationTestBuilder(webDriver, testSetup, directKeycloakAccessSetup,
                                                                    currentRealm).buildDynamicTests());
    dynamicTests.addAll(new ResourceTypeListTestBuilder(webDriver, testSetup, directKeycloakAccessSetup,
                                                        currentRealm).buildDynamicTests());
    for ( String resourceTypeName : Arrays.asList(ResourceTypeNames.USER, ResourceTypeNames.GROUPS) )
    {
      dynamicTests.addAll(new ResourceTypeConfigTestBuilder(webDriver, testSetup, directKeycloakAccessSetup,
                                                            currentRealm, resourceTypeName).buildDynamicTests());
      dynamicTests.addAll(new ResourceTypeAuthorizationTestBuilder(webDriver, testSetup, directKeycloakAccessSetup,
                                                                   currentRealm, resourceTypeName).buildDynamicTests());
    }
    dynamicTests.add(serviceProviderTestBuilder.getClickScimMenuTest());
    dynamicTests.add(enableScim(false));
    return dynamicTests;
  }

  /**
   * enables or disables scim on the service provider configuration
   */
  private DynamicTest enableScim(boolean enable)
  {
    return DynamicTest.dynamicTest(String.format("%s SCIM", enable ? "enabled" : "disable"), () -> {
      setCheckboxElement(By.id("enabled"), enable);
      WebElement saveButton = wait.until(d -> d.findElement(By.id("save")));
      saveButton.click();
      getKeycloakCheckboxElement(By.id("enabled")); // wait until page is completely rebuilt
    });
  }
}
