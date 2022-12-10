package de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder;

import java.time.Duration;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DynamicTest;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.TestSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup.DirectKeycloakAccessSetup;


/**
 * @author Pascal Knueppel
 * @since 12.12.2020
 */
public abstract class AbstractTestBuilder
{

  /**
   * helper object to wait for conditions that should be fulfilled
   */
  protected final WebDriverWait wait;

  /**
   * the current web driver that is used to test the web admin console
   */
  protected final WebDriver webDriver;

  /**
   * a keycloak mock setup that grants direct database access with keycloak objects
   */
  protected final DirectKeycloakAccessSetup directKeycloakAccessSetup;

  /**
   * the test setup that holds information of the currently running server instances
   */
  protected final TestSetup testSetup;

  public AbstractTestBuilder(WebDriver webDriver,
                             TestSetup testSetup,
                             DirectKeycloakAccessSetup directKeycloakAccessSetup)
  {
    this.webDriver = webDriver;
    this.testSetup = testSetup;
    this.directKeycloakAccessSetup = directKeycloakAccessSetup;
    this.wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
  }

  /**
   * @return the tests that should be created by this implementation
   */
  public abstract List<DynamicTest> buildDynamicTests();

  /**
   * the keycloak checkbox elements have their id values on an element that is not clickable. The clickable
   * element is the parent element of the element that is being selected by the id
   * 
   * @param idSelector the id selector for the checkbox element
   * @return the clickable element
   */
  protected WebElement getKeycloakCheckboxElement(By idSelector)
  {
    WebElement enabledButton = wait.until(d -> d.findElement(idSelector));
    return enabledButton.findElement(By.xpath("./.."));
  }

  /**
   * tries to enable a checkbox element that can be found under the given id selector. If the checkbox is
   * already enabled nothing is done
   * 
   * @param idSelector the id selector to find the checkbox
   * @return true if the checkbox was disabled and has been enabled, false if the checkbox was already enabled
   */
  protected boolean enableKeycloakCheckboxElement(By idSelector)
  {
    WebElement checkbox = getKeycloakCheckboxElement(idSelector);
    // this class tells that the checkbox is disabled
    final String disabledClass = "ng-empty";
    if (hasClass(checkbox.findElement(By.tagName("input")), disabledClass))
    {
      checkbox.click();
      return true;
    }
    return false;
  }

  /**
   * tries to disable a checkbox element that can be found under the given id selector. If the checkbox is
   * already disabled nothing is done
   * 
   * @param idSelector the id selector to find the checkbox
   * @return true if the checkbox was enabled and has been disabled, false if the checkbox was already disabled
   */
  protected boolean disableKeycloakCheckboxElement(By idSelector)
  {
    WebElement checkbox = getKeycloakCheckboxElement(idSelector);
    // this class tells that the checkbox is enabled
    final String enabledClass = "ng-not-empty";
    if (hasClass(checkbox.findElement(By.tagName("input")), enabledClass))
    {
      checkbox.click();
      return true;
    }
    return false;
  }

  /**
   * tries to set the checkbox element to the given value
   * 
   * @param idSelector the id selector to find the checkbox
   * @param enabled true to enable the checkbox, false to disable the checkbox
   * @return true if the checkbox state has changed, false if the checkbox was already in the given state
   */
  protected boolean setCheckboxElement(By idSelector, boolean enabled)
  {
    if (enabled)
    {
      return enableKeycloakCheckboxElement(idSelector);
    }
    return disableKeycloakCheckboxElement(idSelector);
  }

  /**
   * checks if the give web element has the given class in the class-attribute
   */
  protected boolean hasClass(WebElement element, String expectedClassValue)
  {
    String classes = element.getAttribute("class");
    for ( String c : classes.split(" ") )
    {
      if (c.equals(expectedClassValue))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * allows to wait until an element is clickable after an action on the web page has modified the DOM document.
   */
  protected WebElement untilClickable(By what)
  {
    return wait.ignoring(StaleElementReferenceException.class).until(ExpectedConditions.elementToBeClickable(what));
  }

  /**
   * @return a test that will simply load the service provider configuration in the web admin console
   */
  public DynamicTest getClickScimMenuTest()
  {
    return DynamicTest.dynamicTest("load SCIM Service Provider configuration", () -> {
      final By xpathScimMenuItem = By.xpath("//li[@id= 'scim-menu']/a");
      WebElement scimMenuLink = wait.until(d -> d.findElement(xpathScimMenuItem));
      MatcherAssert.assertThat(scimMenuLink.getAttribute("href"),
                               Matchers.matchesPattern(".*?#/realms/[\\w-]+?/scim/service-provider/settings"));
      untilClickable(xpathScimMenuItem).click();
      wait.until(d -> d.findElement(By.id("enabled")));
    });
  }

}
