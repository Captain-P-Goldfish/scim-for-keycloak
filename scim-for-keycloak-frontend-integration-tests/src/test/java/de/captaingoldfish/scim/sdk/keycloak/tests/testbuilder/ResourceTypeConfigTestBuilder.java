package de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.TestSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup.DirectKeycloakAccessSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.utils.WaitStrategy;


/**
 * @author Pascal Knueppel
 * @since 21.12.2020
 */
public class ResourceTypeConfigTestBuilder extends AbstractTestBuilder
{

  /**
   * the current realm that is under test
   */
  private final String currentRealm;

  /**
   * the resource type under test
   */
  private final String resourceTypeName;

  /**
   * used as holder variable to remember the original description value during the tests
   */
  private String originalResourceTypeDescription;

  public ResourceTypeConfigTestBuilder(WebDriver webDriver,
                                       TestSetup testSetup,
                                       DirectKeycloakAccessSetup directKeycloakAccessSetup,
                                       String realm,
                                       String resourceTypeName)
  {
    super(webDriver, testSetup, directKeycloakAccessSetup);
    this.currentRealm = realm;
    this.resourceTypeName = resourceTypeName;
  }

  /**
   * generates selenium tests for resource-type.html
   */
  @Override
  public List<DynamicTest> buildDynamicTests()
  {
    final String description = resourceTypeName + "-test-description";

    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ******************************************************************************************************* */
    dynamicTests.add(getClickScimMenuTest());
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("open resource type definition: " + resourceTypeName, () -> {
      WebElement resourceTypeListTab = untilClickable(By.id("resource-types-list-tab"));
      resourceTypeListTab.click();
      wait.until(d -> d.findElement(By.id("meta-resource-type-table")));

      WebElement resourceTypesTable = webDriver.findElement(By.id("resource-type-table"));
      WebElement tableBody = resourceTypesTable.findElement(By.tagName("tbody"));
      List<WebElement> rows = tableBody.findElements(By.tagName("tr"));

      WebElement resourceTypeColumn = rows.stream()
                                          .map(row -> row.findElements(By.tagName("td")).get(0))
                                          .filter(column -> column.getText().equals(resourceTypeName))
                                          .findAny()
                                          .orElseThrow(IllegalStateException::new);
      resourceTypeColumn.findElement(By.tagName("a")).click();
      wait.until(d -> d.findElement(By.id("resourceTypeDescription")));
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("modify resource type definition:" + resourceTypeName, () -> {
      WebElement descriptionElement = webDriver.findElement(By.id("resourceTypeDescription"));
      originalResourceTypeDescription = descriptionElement.getText();
      descriptionElement.clear();
      descriptionElement.sendKeys(description);
      Assertions.assertTrue(setCheckboxElement(By.id("autoFiltering"), TestValues.AUTO_FILTERING));
      Assertions.assertTrue(setCheckboxElement(By.id("autoSorting"), TestValues.AUTO_SORTING));
      Assertions.assertTrue(setCheckboxElement(By.id("etagEnabled"), TestValues.AUTO_ETAG));
      Assertions.assertTrue(setCheckboxElement(By.id("disableCreate"), TestValues.CREATE_DISABLED));
      Assertions.assertTrue(setCheckboxElement(By.id("disableGet"), TestValues.GET_DISABLED));
      Assertions.assertTrue(setCheckboxElement(By.id("disableList"), TestValues.LIST_DISABLED));
      Assertions.assertTrue(setCheckboxElement(By.id("disableUpdate"), TestValues.UPDATE_DISABLED));
      Assertions.assertTrue(setCheckboxElement(By.id("disableDelete"), TestValues.DELETE_DISABLED));
      Assertions.assertTrue(setCheckboxElement(By.id("resourceTypeEnabled"), TestValues.ENABLED));

      WebElement saveButton = wait.until(d -> d.findElement(By.id("save")));
      saveButton.click();
      getKeycloakCheckboxElement(By.id("resourceTypeEnabled")); // wait until page is completely rebuilt
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("check database values of resource type: " + resourceTypeName, () -> {
      new WaitStrategy().waitFor(() -> {
        directKeycloakAccessSetup.clearCache();
        List<ScimResourceTypeEntity> resourceTypeList = directKeycloakAccessSetup.getResourceTypeEntities(currentRealm);
        ScimResourceTypeEntity resourceType = resourceTypeList.stream()
                                                              .filter(rt -> rt.getName().equals(resourceTypeName))
                                                              .findAny()
                                                              .orElseThrow(IllegalStateException::new);
        Assertions.assertEquals(description, resourceType.getDescription());
        Assertions.assertEquals(TestValues.ENABLED, resourceType.isEnabled());
        Assertions.assertEquals(TestValues.AUTO_FILTERING, resourceType.isAutoFiltering());
        Assertions.assertEquals(TestValues.AUTO_SORTING, resourceType.isAutoSorting());
        Assertions.assertEquals(TestValues.AUTO_ETAG, resourceType.isEtagEnabled());
        Assertions.assertEquals(TestValues.CREATE_DISABLED, resourceType.isDisableCreate());
        Assertions.assertEquals(TestValues.GET_DISABLED, resourceType.isDisableGet());
        Assertions.assertEquals(TestValues.LIST_DISABLED, resourceType.isDisableList());
        Assertions.assertEquals(TestValues.UPDATE_DISABLED, resourceType.isDisableUpdate());
        Assertions.assertEquals(TestValues.DELETE_DISABLED, resourceType.isDisableDelete());

        for ( ScimResourceTypeEntity resourceTypeEntity : resourceTypeList )
        {
          if (resourceTypeName.equals(resourceTypeEntity.getName()))
          {
            continue;
          }
          Assertions.assertNotEquals(description, resourceTypeEntity.getDescription());
          Assertions.assertNotEquals(TestValues.ENABLED, resourceTypeEntity.isEnabled());
          Assertions.assertNotEquals(TestValues.AUTO_FILTERING, resourceTypeEntity.isAutoFiltering());
          Assertions.assertNotEquals(TestValues.AUTO_SORTING, resourceTypeEntity.isAutoSorting());
          Assertions.assertNotEquals(TestValues.AUTO_ETAG, resourceTypeEntity.isEtagEnabled());
          Assertions.assertNotEquals(TestValues.CREATE_DISABLED, resourceTypeEntity.isDisableCreate());
          Assertions.assertNotEquals(TestValues.GET_DISABLED, resourceTypeEntity.isDisableGet());
          Assertions.assertNotEquals(TestValues.LIST_DISABLED, resourceTypeEntity.isDisableList());
          Assertions.assertNotEquals(TestValues.UPDATE_DISABLED, resourceTypeEntity.isDisableUpdate());
          Assertions.assertNotEquals(TestValues.DELETE_DISABLED, resourceTypeEntity.isDisableDelete());
        }
      });
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("reset resource type definition: " + resourceTypeName, () -> {
      Assertions.assertTrue(setCheckboxElement(By.id("resourceTypeEnabled"), ResetValues.ENABLED));
      Assertions.assertTrue(setCheckboxElement(By.id("autoFiltering"), ResetValues.AUTO_FILTERING));
      Assertions.assertTrue(setCheckboxElement(By.id("autoSorting"), ResetValues.AUTO_SORTING));
      Assertions.assertTrue(setCheckboxElement(By.id("etagEnabled"), ResetValues.AUTO_ETAG));
      Assertions.assertTrue(setCheckboxElement(By.id("disableCreate"), ResetValues.CREATE_DISABLED));
      Assertions.assertTrue(setCheckboxElement(By.id("disableGet"), ResetValues.GET_DISABLED));
      Assertions.assertTrue(setCheckboxElement(By.id("disableList"), ResetValues.LIST_DISABLED));
      Assertions.assertTrue(setCheckboxElement(By.id("disableUpdate"), ResetValues.UPDATE_DISABLED));
      Assertions.assertTrue(setCheckboxElement(By.id("disableDelete"), ResetValues.DELETE_DISABLED));

      WebElement descriptionElement = webDriver.findElement(By.id("resourceTypeDescription"));
      descriptionElement.clear();
      descriptionElement.sendKeys(originalResourceTypeDescription);

      WebElement saveButton = wait.until(d -> d.findElement(By.id("save")));
      saveButton.click();
      getKeycloakCheckboxElement(By.id("resourceTypeEnabled")); // wait until page is completely rebuilt
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("check database values of resource type: " + resourceTypeName, () -> {
      new WaitStrategy().waitFor(() -> {
        directKeycloakAccessSetup.clearCache();
        List<ScimResourceTypeEntity> resourceTypeList = directKeycloakAccessSetup.getResourceTypeEntities(currentRealm);
        ScimResourceTypeEntity resourceType = resourceTypeList.stream()
                                                              .filter(rt -> rt.getName().equals(resourceTypeName))
                                                              .findAny()
                                                              .orElseThrow(IllegalStateException::new);
        Assertions.assertEquals(originalResourceTypeDescription, resourceType.getDescription());
        Assertions.assertEquals(ResetValues.ENABLED, resourceType.isEnabled());
        Assertions.assertEquals(ResetValues.AUTO_FILTERING, resourceType.isAutoFiltering());
        Assertions.assertEquals(ResetValues.AUTO_SORTING, resourceType.isAutoSorting());
        Assertions.assertEquals(ResetValues.AUTO_ETAG, resourceType.isEtagEnabled());
        Assertions.assertEquals(ResetValues.CREATE_DISABLED, resourceType.isDisableCreate());
        Assertions.assertEquals(ResetValues.GET_DISABLED, resourceType.isDisableGet());
        Assertions.assertEquals(ResetValues.LIST_DISABLED, resourceType.isDisableList());
        Assertions.assertEquals(ResetValues.UPDATE_DISABLED, resourceType.isDisableUpdate());
        Assertions.assertEquals(ResetValues.DELETE_DISABLED, resourceType.isDisableDelete());
      });
    }));
    /* ******************************************************************************************************* */
    return dynamicTests;
  }

  private static class ResetValues
  {

    private static final boolean ENABLED = true;

    private static final boolean AUTO_FILTERING = true;

    private static final boolean AUTO_SORTING = true;

    private static final boolean AUTO_ETAG = false;

    private static final boolean CREATE_DISABLED = false;

    private static final boolean GET_DISABLED = false;

    private static final boolean LIST_DISABLED = false;

    private static final boolean UPDATE_DISABLED = false;

    private static final boolean DELETE_DISABLED = false;
  }

  private static class TestValues
  {

    private static final boolean ENABLED = false;

    private static final boolean AUTO_FILTERING = false;

    private static final boolean AUTO_SORTING = false;

    private static final boolean AUTO_ETAG = true;

    private static final boolean CREATE_DISABLED = true;

    private static final boolean GET_DISABLED = true;

    private static final boolean LIST_DISABLED = true;

    private static final boolean UPDATE_DISABLED = true;

    private static final boolean DELETE_DISABLED = true;
  }
}
