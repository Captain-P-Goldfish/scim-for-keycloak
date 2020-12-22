package de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.openqa.selenium.WebDriver;

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
    dynamicTests.addAll(new ServiceProviderConfigTestBuilder(webDriver, testSetup, directKeycloakAccessSetup,
                                                             currentRealm).buildDynamicTests());
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
    return dynamicTests;
  }
}
