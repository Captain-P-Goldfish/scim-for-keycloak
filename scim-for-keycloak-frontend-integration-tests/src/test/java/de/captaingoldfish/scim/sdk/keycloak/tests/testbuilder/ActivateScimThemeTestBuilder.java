package de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.TestSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup.DirectKeycloakAccessSetup;


/**
 * @author Pascal Knueppel
 * @since 12.12.2020
 */
public class ActivateScimThemeTestBuilder extends AbstractTestBuilder
{

  private static final String SCIM_THEME_NAME = "scim";

  public ActivateScimThemeTestBuilder(WebDriver webDriver,
                                      TestSetup testSetup,
                                      DirectKeycloakAccessSetup directKeycloakAccessSetup)
  {
    super(webDriver, testSetup, directKeycloakAccessSetup);
  }

  /**
   * loads the theme tab in the realm menu and selects the scim theme that activates the SCIM menu entry
   */
  @Override
  public List<DynamicTest> buildDynamicTests()
  {
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(DynamicTest.dynamicTest("select SCIM theme", () -> {
      By xpathRealmSettingsLink = By.xpath("//a[text()[contains(.,'Realm Settings')]]");
      wait.until(d -> ExpectedConditions.not(ExpectedConditions.stalenessOf(d.findElement(xpathRealmSettingsLink))));
      untilClickable(xpathRealmSettingsLink).click();
      untilClickable(By.xpath(".//a[text() = 'Themes']")).click();

      Select adminThemeSelector = wait.until(d -> new Select(d.findElement(By.id("adminTheme"))));
      adminThemeSelector.selectByValue("string:" + SCIM_THEME_NAME);
      WebElement saveButton = webDriver.findElement(By.xpath("//button[@kc-save]"));
      saveButton.click();
      webDriver.navigate().refresh();
    }));
    dynamicTests.add(getClickScimMenuTest());
    return dynamicTests;
  }
}
