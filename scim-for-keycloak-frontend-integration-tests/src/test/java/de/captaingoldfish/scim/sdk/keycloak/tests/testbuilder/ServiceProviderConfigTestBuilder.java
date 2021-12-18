package de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.TestSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup.DirectKeycloakAccessSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.utils.WaitStrategy;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 18.12.2020
 */
@Slf4j
public class ServiceProviderConfigTestBuilder extends AbstractTestBuilder
{

  /**
   * necessary to select the {@link ScimServiceProviderEntity} from the database
   */
  private final String currentRealm;

  public ServiceProviderConfigTestBuilder(WebDriver webDriver,
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
    compareTestAndResetValues();

    List<DynamicTest> dynamicTests = new ArrayList<>();

    /* ******************************************************************************************************* */
    dynamicTests.add(getClickScimMenuTest());
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("reset data to default settings", () -> {
      WebElement bulkSupportedCheckbox = webDriver.findElement(By.id("bulkSupported"));
      if (!bulkSupportedCheckbox.isSelected())
      {
        Assertions.assertTrue(setCheckboxElement(By.id("bulkSupported"), ResetValues.BULK_SUPPORTED));
        WebElement saveButton = wait.until(d -> d.findElement(By.id("save")));
        saveButton.click();
        getKeycloakCheckboxElement(By.id("enabled")); // wait until page is completely rebuilt
      }
      setCheckboxElement(By.id("enabled"), ResetValues.ENABLED);
      setCheckboxElement(By.id("filterSupported"), ResetValues.FILTER_SUPPORTED);
      setCheckboxElement(By.id("sortSupported"), ResetValues.SORT_SUPPORTED);
      setCheckboxElement(By.id("patchSupported"), ResetValues.PATCH_SUPPORTED);
      setCheckboxElement(By.id("etagSupported"), ResetValues.ETAG_SUPPORTED);
      setCheckboxElement(By.id("changePasswordSupported"), ResetValues.CHANGE_PASSWORD_SUPPORTED);
      setCheckboxElement(By.id("bulkSupported"), ResetValues.BULK_SUPPORTED);
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("change values of service provider", () -> {
      WebElement maxResultsInput = webDriver.findElement(By.id("filterMaxResults"));
      maxResultsInput.clear();
      maxResultsInput.sendKeys(String.valueOf(TestValues.MAX_RESULTS));

      WebElement maxOperationsInput = webDriver.findElement(By.id("bulkMaxOperations"));
      maxOperationsInput.clear();
      maxOperationsInput.sendKeys(String.valueOf(TestValues.BULK_MAX_OPERATIONS));

      WebElement maxPayloadInput = webDriver.findElement(By.id("maxPayloadSize"));
      maxPayloadInput.clear();
      maxPayloadInput.sendKeys(String.valueOf(TestValues.BULK_MAX_PAYLOAD));


      Assertions.assertTrue(setCheckboxElement(By.id("enabled"), TestValues.ENABLED));
      Assertions.assertTrue(setCheckboxElement(By.id("filterSupported"), TestValues.FILTER_SUPPORTED));
      Assertions.assertTrue(setCheckboxElement(By.id("sortSupported"), TestValues.SORT_SUPPORTED));
      Assertions.assertTrue(setCheckboxElement(By.id("patchSupported"), TestValues.PATCH_SUPPORTED));
      Assertions.assertTrue(setCheckboxElement(By.id("etagSupported"), TestValues.ETAG_SUPPORTED));
      Assertions.assertTrue(setCheckboxElement(By.id("changePasswordSupported"), TestValues.CHANGE_PASSWORD_SUPPORTED));
      Assertions.assertTrue(setCheckboxElement(By.id("bulkSupported"), TestValues.BULK_SUPPORTED));


      WebElement saveButton = wait.until(d -> d.findElement(By.id("save")));
      saveButton.click();
      getKeycloakCheckboxElement(By.id("enabled")); // wait until page is completely rebuilt
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("check database values of service provider", () -> {
      new WaitStrategy().waitFor(() -> {
        directKeycloakAccessSetup.clearCache();
        ScimServiceProviderEntity serviceProviderEntity = directKeycloakAccessSetup.getServiceProviderEntity(currentRealm);
        Assertions.assertEquals(TestValues.ENABLED, serviceProviderEntity.isEnabled());
        Assertions.assertEquals(TestValues.MAX_RESULTS, serviceProviderEntity.getFilterMaxResults());
        Assertions.assertEquals(TestValues.FILTER_SUPPORTED, serviceProviderEntity.isFilterSupported());
        Assertions.assertEquals(TestValues.SORT_SUPPORTED, serviceProviderEntity.isSortSupported());
        Assertions.assertEquals(TestValues.PATCH_SUPPORTED, serviceProviderEntity.isPatchSupported());
        Assertions.assertEquals(TestValues.ETAG_SUPPORTED, serviceProviderEntity.isEtagSupported());
        Assertions.assertEquals(TestValues.CHANGE_PASSWORD_SUPPORTED,
                                serviceProviderEntity.isChangePasswordSupported());
        Assertions.assertEquals(TestValues.BULK_SUPPORTED, serviceProviderEntity.isBulkSupported());
        Assertions.assertEquals(TestValues.BULK_MAX_OPERATIONS, serviceProviderEntity.getBulkMaxOperations());
        Assertions.assertEquals(TestValues.BULK_MAX_PAYLOAD, serviceProviderEntity.getBulkMaxPayloadSize());
      });
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("reset values of service provider to default", () -> {
      // the inputs fields are disabled until the enabled checkbox has been enabled again and the config is saved
      {
        Assertions.assertTrue(setCheckboxElement(By.id("enabled"), ResetValues.ENABLED));
        WebElement saveButton = wait.until(d -> d.findElement(By.id("save")));
        saveButton.click();

        Assertions.assertTrue(setCheckboxElement(By.id("bulkSupported"), ResetValues.BULK_SUPPORTED));
        saveButton = wait.until(d -> d.findElement(By.id("save")));
        saveButton.click();
      }

      Assertions.assertTrue(setCheckboxElement(By.id("filterSupported"), ResetValues.FILTER_SUPPORTED));
      Assertions.assertTrue(setCheckboxElement(By.id("sortSupported"), ResetValues.SORT_SUPPORTED));
      Assertions.assertTrue(setCheckboxElement(By.id("patchSupported"), ResetValues.PATCH_SUPPORTED));
      Assertions.assertTrue(setCheckboxElement(By.id("etagSupported"), ResetValues.ETAG_SUPPORTED));
      Assertions.assertTrue(setCheckboxElement(By.id("changePasswordSupported"),
                                               ResetValues.CHANGE_PASSWORD_SUPPORTED));
      Assertions.assertTrue(setCheckboxElement(By.id("bulkSupported"), ResetValues.BULK_SUPPORTED));

      WebElement maxResultsInput = webDriver.findElement(By.id("filterMaxResults"));
      maxResultsInput.clear();
      maxResultsInput.sendKeys(String.valueOf(ResetValues.MAX_RESULTS));

      WebElement maxOperationsInput = webDriver.findElement(By.id("bulkMaxOperations"));
      maxOperationsInput.clear();
      maxOperationsInput.sendKeys(String.valueOf(ResetValues.BULK_MAX_OPERATIONS));

      WebElement maxPayloadInput = webDriver.findElement(By.id("maxPayloadSize"));
      maxPayloadInput.clear();
      maxPayloadInput.sendKeys(String.valueOf(ResetValues.BULK_MAX_PAYLOAD));

      WebElement saveButton = wait.until(d -> d.findElement(By.id("save")));
      saveButton.click();

      getKeycloakCheckboxElement(By.id("enabled")); // wait until page is completely rebuilt
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("check database values of service provider", () -> {
      new WaitStrategy().waitFor(() -> {
        directKeycloakAccessSetup.clearCache();
        ScimServiceProviderEntity serviceProviderEntity = directKeycloakAccessSetup.getServiceProviderEntity(currentRealm);
        Assertions.assertEquals(ResetValues.ENABLED, serviceProviderEntity.isEnabled());
        Assertions.assertEquals(ResetValues.MAX_RESULTS, serviceProviderEntity.getFilterMaxResults());
        Assertions.assertEquals(ResetValues.FILTER_SUPPORTED, serviceProviderEntity.isFilterSupported());
        Assertions.assertEquals(ResetValues.SORT_SUPPORTED, serviceProviderEntity.isSortSupported());
        Assertions.assertEquals(ResetValues.PATCH_SUPPORTED, serviceProviderEntity.isPatchSupported());
        Assertions.assertEquals(ResetValues.ETAG_SUPPORTED, serviceProviderEntity.isEtagSupported());
        Assertions.assertEquals(ResetValues.CHANGE_PASSWORD_SUPPORTED,
                                serviceProviderEntity.isChangePasswordSupported());
        Assertions.assertEquals(ResetValues.BULK_SUPPORTED, serviceProviderEntity.isBulkSupported());
        Assertions.assertEquals(ResetValues.BULK_MAX_OPERATIONS, serviceProviderEntity.getBulkMaxOperations());
        Assertions.assertEquals(ResetValues.BULK_MAX_PAYLOAD, serviceProviderEntity.getBulkMaxPayloadSize());
      });
    }));
    /* ******************************************************************************************************* */
    return dynamicTests;
  }

  /**
   * this test will simply make sure that the values of the test values and the reset values are not identical
   */
  public void compareTestAndResetValues()
  {
    Assertions.assertNotEquals(ResetValues.ENABLED, TestValues.ENABLED);
    Assertions.assertNotEquals(ResetValues.MAX_RESULTS, TestValues.MAX_RESULTS);
    Assertions.assertNotEquals(ResetValues.FILTER_SUPPORTED, TestValues.FILTER_SUPPORTED);
    Assertions.assertNotEquals(ResetValues.SORT_SUPPORTED, TestValues.SORT_SUPPORTED);
    Assertions.assertNotEquals(ResetValues.PATCH_SUPPORTED, TestValues.PATCH_SUPPORTED);
    Assertions.assertNotEquals(ResetValues.ETAG_SUPPORTED, TestValues.ETAG_SUPPORTED);
    Assertions.assertNotEquals(ResetValues.CHANGE_PASSWORD_SUPPORTED, TestValues.CHANGE_PASSWORD_SUPPORTED);
    Assertions.assertNotEquals(ResetValues.BULK_SUPPORTED, TestValues.BULK_SUPPORTED);
    Assertions.assertNotEquals(ResetValues.BULK_MAX_OPERATIONS, TestValues.BULK_MAX_OPERATIONS);
    Assertions.assertNotEquals(ResetValues.BULK_MAX_PAYLOAD, TestValues.BULK_MAX_PAYLOAD);
  }

  private static final class ResetValues
  {

    private static final boolean ENABLED = true;

    private static final int MAX_RESULTS = 50;

    private static final boolean FILTER_SUPPORTED = true;

    private static final boolean SORT_SUPPORTED = true;

    private static final boolean PATCH_SUPPORTED = true;

    private static final boolean ETAG_SUPPORTED = true;

    private static final boolean CHANGE_PASSWORD_SUPPORTED = false;

    private static final boolean BULK_SUPPORTED = true;

    private static final int BULK_MAX_OPERATIONS = 10;

    private static final long BULK_MAX_PAYLOAD = 1024 * 1024 * 2;

  }

  private static final class TestValues
  {

    private static final boolean ENABLED = false;

    private static final int MAX_RESULTS = 77;

    private static final boolean FILTER_SUPPORTED = false;

    private static final boolean SORT_SUPPORTED = false;

    private static final boolean PATCH_SUPPORTED = false;

    private static final boolean ETAG_SUPPORTED = false;

    private static final boolean CHANGE_PASSWORD_SUPPORTED = true;

    private static final boolean BULK_SUPPORTED = false;

    private static final int BULK_MAX_OPERATIONS = 50;

    private static final long BULK_MAX_PAYLOAD = 1024 * 1024;

  }
}
