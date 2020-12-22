package de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.TestSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup.DirectKeycloakAccessSetup;


/**
 * @author Pascal Knueppel
 * @since 21.12.2020
 */
public class ResourceTypeListTestBuilder extends AbstractTestBuilder
{

  /**
   * the realm that for which the resource-type-list is under test
   */
  private String currentRealm;

  public ResourceTypeListTestBuilder(WebDriver webDriver,
                                     TestSetup testSetup,
                                     DirectKeycloakAccessSetup directKeycloakAccessSetup,
                                     String realm)
  {
    super(webDriver, testSetup, directKeycloakAccessSetup);
    this.currentRealm = realm;
  }

  /**
   * generates dynamic selenium tests for the resource-type-list.html file
   */
  @Override
  public List<DynamicTest> buildDynamicTests()
  {
    List<DynamicTest> dynamicTests = new ArrayList<>();

    /* ******************************************************************************************************* */
    dynamicTests.add(getClickScimMenuTest());
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("load resource types list", () -> {
      WebElement resourceTypeListTab = untilClickable(By.id("resource-types-list-tab"));
      resourceTypeListTab.click();
      wait.until(d -> d.findElement(By.id("meta-resource-type-table")));
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("check all resource type configurations are identical", () -> {
      List<ScimResourceTypeEntity> resourceTypeEntities = directKeycloakAccessSetup.getResourceTypeEntities(currentRealm);
      ScimResourceTypeEntity firstResourceType = resourceTypeEntities.get(0);
      for ( int i = 1 ; i < resourceTypeEntities.size() ; i++ )
      {
        ScimResourceTypeEntity nextResourceType = resourceTypeEntities.get(i);
        Assertions.assertEquals(firstResourceType.isEnabled(), nextResourceType.isEnabled());
        Assertions.assertEquals(firstResourceType.isAutoFiltering(), nextResourceType.isAutoFiltering());
        Assertions.assertEquals(firstResourceType.isAutoSorting(), nextResourceType.isAutoSorting());
        Assertions.assertEquals(firstResourceType.isEtagEnabled(), nextResourceType.isEtagEnabled());
        Assertions.assertEquals(firstResourceType.isDisableCreate(), nextResourceType.isDisableCreate());
        Assertions.assertEquals(firstResourceType.isDisableGet(), nextResourceType.isDisableGet());
        Assertions.assertEquals(firstResourceType.isDisableList(), nextResourceType.isDisableList());
        Assertions.assertEquals(firstResourceType.isDisableUpdate(), nextResourceType.isDisableUpdate());
        Assertions.assertEquals(firstResourceType.isDisableDelete(), nextResourceType.isDisableDelete());
      }
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("check displayed meta resource types", () -> {
      WebElement metaResourceTypesTable = webDriver.findElement(By.id("meta-resource-type-table"));
      WebElement tableBody = metaResourceTypesTable.findElement(By.tagName("tbody"));
      List<WebElement> rows = tableBody.findElements(By.tagName("tr"));
      Assertions.assertEquals(3, rows.size());
      List<String> metaResourceTypeIds = rows.stream()
                                             .map(r -> r.findElements(By.tagName("td")).get(0))
                                             .map(WebElement::getText)
                                             .collect(Collectors.toList());
      MatcherAssert.assertThat(metaResourceTypeIds,
                               Matchers.containsInAnyOrder(Matchers.equalTo(ResourceTypeNames.SERVICE_PROVIDER_CONFIG),
                                                           Matchers.equalTo(ResourceTypeNames.RESOURCE_TYPE),
                                                           Matchers.equalTo(ResourceTypeNames.SCHEMA)));

      for ( WebElement row : rows )
      {
        List<WebElement> columns = row.findElements(By.tagName("td"));
        Assertions.assertEquals(2, columns.size());
        String resourceTypeName = columns.get(0).getText();
        WebElement urlColumn = columns.get(1);

        final String expectedUrlPattern = String.format("^https?://.+?/auth/realms/%s/scim/v2/%ss?$",
                                                        currentRealm,
                                                        resourceTypeName);
        MatcherAssert.assertThat(urlColumn.getText(), Matchers.matchesPattern(expectedUrlPattern));
        MatcherAssert.assertThat(urlColumn.findElement(By.tagName("a")).getAttribute("href"),
                                 Matchers.matchesPattern(expectedUrlPattern));
      }

    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("check displayed resource types", () -> {
      WebElement resourceTypesTable = webDriver.findElement(By.id("resource-type-table"));
      List<ScimResourceTypeEntity> resourceTypeEntities = directKeycloakAccessSetup.getResourceTypeEntities(currentRealm);

      WebElement tableBody = resourceTypesTable.findElement(By.tagName("tbody"));
      List<WebElement> rows = tableBody.findElements(By.tagName("tr"));
      Assertions.assertEquals(3, rows.size());
      List<String> resourceTypeIds = rows.stream()
                                         .map(r -> r.findElements(By.tagName("td")).get(0))
                                         .map(WebElement::getText)
                                         .collect(Collectors.toList());
      Assertions.assertEquals(resourceTypeEntities.size(), resourceTypeIds.size());
      List<String> databaseResourceTypeIds = resourceTypeEntities.stream()
                                                                 .map(ScimResourceTypeEntity::getName)
                                                                 .collect(Collectors.toList());
      MatcherAssert.assertThat(resourceTypeIds,
                               Matchers.containsInAnyOrder(databaseResourceTypeIds.stream()
                                                                                  .map(Matchers::equalTo)
                                                                                  .collect(Collectors.toList())));

      for ( WebElement row : rows )
      {
        List<WebElement> columns = row.findElements(By.tagName("td"));
        Assertions.assertEquals(5, columns.size());
        String resourceTypeName = columns.get(0).getText();
        ScimResourceTypeEntity resourceTypeEntity = resourceTypeEntities.stream()
                                                                        .filter(rt -> rt.getName()
                                                                                        .equals(resourceTypeName))
                                                                        .findAny()
                                                                        .orElseThrow(IllegalStateException::new);

        final String expectedWebAdminConfigUrlPattern = String.format("^https?://.+?/auth/admin/master/console"
                                                                      + "/#/realms/%s/scim/resource-type/%s",
                                                                      currentRealm,
                                                                      resourceTypeName);
        MatcherAssert.assertThat(columns.get(0).findElement(By.tagName("a")).getAttribute("href"),
                                 Matchers.matchesPattern(expectedWebAdminConfigUrlPattern));
        final String expectedScimUrlPattern = String.format("^https?://.+?/auth/realms/%s/scim/v2/%ss?$",
                                                            currentRealm,
                                                            resourceTypeName);
        MatcherAssert.assertThat(columns.get(1).getText(), Matchers.matchesPattern(expectedScimUrlPattern));
        MatcherAssert.assertThat(columns.get(1).findElement(By.tagName("a")).getAttribute("href"),
                                 Matchers.matchesPattern(expectedScimUrlPattern));
        Assertions.assertEquals(columns.get(2).getText(), String.valueOf(resourceTypeEntity.isEnabled()));
        Assertions.assertEquals(columns.get(3).getText(), String.valueOf(resourceTypeEntity.isRequireAuthentication()));
        Assertions.assertEquals(columns.get(4).getAttribute("kc-open"),
                                String.format("/realms/%s/scim/resource-type/%s", currentRealm, resourceTypeName));
      }
    }));
    /* ******************************************************************************************************* */
    return dynamicTests;
  }
}
