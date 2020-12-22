package de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.TestSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup.DirectKeycloakAccessSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.utils.WaitStrategy;


/**
 * @author Pascal Knueppel
 * @since 21.12.2020
 */
public class ResourceTypeAuthorizationTestBuilder extends AbstractTestBuilder
{

  /**
   * the current realm that is under test
   */
  private final String currentRealm;

  /**
   * the resource type under test
   */
  private final String resourceTypeName;

  public ResourceTypeAuthorizationTestBuilder(WebDriver webDriver,
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
   * generates selenium tests for resource-type-role-mappings.html
   */
  @Override
  public List<DynamicTest> buildDynamicTests()
  {
    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ******************************************************************************************************* */
    dynamicTests.add(getClickScimMenuTest());
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("open resource type authorization definition: " + resourceTypeName, () -> {
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
      WebElement authorizationTab = wait.until(d -> d.findElement(By.id("resource-type-authorization-tab")));
      authorizationTab.click();
      getKeycloakCheckboxElement(By.id("authenticated"));
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("disable authentication: " + resourceTypeName, () -> {
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fieldset-common-roles")));
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fieldset-create-roles")));
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fieldset-get-roles")));
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fieldset-update-roles")));
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fieldset-delete-roles")));

      Assertions.assertTrue(setCheckboxElement(By.id("authenticated"), false));

      wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("fieldset-common-roles")));
      wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("fieldset-create-roles")));
      wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("fieldset-get-roles")));
      wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("fieldset-update-roles")));
      wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("fieldset-delete-roles")));

      webDriver.findElement(By.id("save")).click();
      getKeycloakCheckboxElement(By.id("authenticated"));
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("enable authentication again: " + resourceTypeName, () -> {
      Assertions.assertTrue(setCheckboxElement(By.id("authenticated"), true));

      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fieldset-common-roles")));
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fieldset-create-roles")));
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fieldset-get-roles")));
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fieldset-update-roles")));
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fieldset-delete-roles")));

      webDriver.findElement(By.id("save")).click();
      getKeycloakCheckboxElement(By.id("authenticated"));
    }));
    /* ******************************************************************************************************* */
    dynamicTests.addAll(getAssignRolesTests("common"));
    dynamicTests.addAll(getAssignRolesTests("create"));
    dynamicTests.addAll(getAssignRolesTests("get"));
    dynamicTests.addAll(getAssignRolesTests("update"));
    dynamicTests.addAll(getAssignRolesTests("delete"));
    /* ******************************************************************************************************* */

    return dynamicTests;
  }

  /**
   * creates tests on assigning roles to the given resource type based on the specific role type
   * 
   * @param roleType one of either [common, create, get, update, delete]
   */
  private List<DynamicTest> getAssignRolesTests(String roleType)
  {
    List<String> tempAssignedRoles = new ArrayList<>();
    Random random = new Random();

    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("add " + roleType + " roles: " + resourceTypeName, () -> {
      wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("available-" + roleType)));
      untilClickable(By.id("fieldset-" + roleType + "-roles")).click();
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("available-" + roleType)));

      WebElement availableRolesSelection = wait.until(d -> d.findElement(By.id("available-" + roleType)));
      Select select = new Select(availableRolesSelection);
      List<WebElement> options = availableRolesSelection.findElements(By.tagName("option"));
      for ( int i = 0 ; i < options.size() / 2 ; i++ )
      {
        final int index = random.nextInt(options.size());
        WebElement roleOption = options.get(index);
        String roleName = roleOption.getText();
        select.selectByValue(roleName);
        options.remove(index);
        tempAssignedRoles.add(roleName);
      }

      WebElement addSelectedButton = untilClickable(By.id("add-" + roleType + "-role"));
      addSelectedButton.click();
      wait.ignoring(StaleElementReferenceException.class).until(d -> d.findElement(By.id("available-" + roleType)));
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("verify select lists are displaying elements correctly", () -> {
      List<String> availableRoles;
      List<String> addedRoles;

      {
        WebElement availableClientSelection = wait.until(d -> d.findElement(By.id("available-" + roleType)));
        List<WebElement> availableOptions = availableClientSelection.findElements(By.tagName("option"));
        availableRoles = availableOptions.stream().map(WebElement::getText).collect(Collectors.toList());
      }

      {
        WebElement assignedClientSelection = wait.until(d -> d.findElement(By.id("assigned-" + roleType)));
        List<WebElement> assignedOptions = assignedClientSelection.findElements(By.tagName("option"));
        addedRoles = assignedOptions.stream().map(WebElement::getText).collect(Collectors.toList());
      }

      MatcherAssert.assertThat(availableRoles,
                               Matchers.not(Matchers.hasItems(tempAssignedRoles.stream()
                                                                               .map(Matchers::equalTo)
                                                                               .collect(Collectors.toList())
                                                                               .toArray(new Matcher[0]))));
      MatcherAssert.assertThat(addedRoles,
                               Matchers.hasItems(tempAssignedRoles.stream()
                                                                  .map(Matchers::equalTo)
                                                                  .collect(Collectors.toList())
                                                                  .toArray(new Matcher[0])));
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("check assigned " + roleType + " roles: " + resourceTypeName, () -> {
      new WaitStrategy().waitFor(() -> {
        directKeycloakAccessSetup.clearCache();
        List<ScimResourceTypeEntity> resourceTypes = directKeycloakAccessSetup.getResourceTypeEntities(currentRealm);
        ScimResourceTypeEntity resourceTypeEntity = resourceTypes.stream()
                                                                 .filter(rt -> rt.getName().equals(resourceTypeName))
                                                                 .findAny()
                                                                 .orElseThrow(IllegalStateException::new);
        List<String> assignedRoles = this.getRoleTypeEntities(roleType, resourceTypeEntity)
                                         .stream()
                                         .map(RoleEntity::getName)
                                         .collect(Collectors.toList());
        MatcherAssert.assertThat(assignedRoles,
                                 Matchers.containsInAnyOrder(tempAssignedRoles.stream()
                                                                              .map(Matchers::equalTo)
                                                                              .collect(Collectors.toList())));
      });
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("remove assigned " + roleType + " roles: " + resourceTypeName, () -> {
      WebElement assignedRolesSelection = wait.until(d -> d.findElement(By.id("assigned-" + roleType)));
      Select select = new Select(assignedRolesSelection);
      List<WebElement> options = assignedRolesSelection.findElements(By.tagName("option"));
      for ( int i = 0 ; i < options.size() ; i++ )
      {
        select.selectByIndex(i);
      }
      WebElement removeSelectedButton = untilClickable(By.id("remove-" + roleType + "-role"));
      removeSelectedButton.click();
      wait.until(d -> d.findElement(By.id("available-" + roleType)));
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("verify select lists are displaying elements correctly", () -> {
      List<String> availableRoles;
      List<String> addedRoles;

      {
        WebElement availableClientSelection = wait.until(d -> d.findElement(By.id("available-" + roleType)));
        List<WebElement> availableOptions = availableClientSelection.findElements(By.tagName("option"));
        availableRoles = availableOptions.stream().map(WebElement::getText).collect(Collectors.toList());
      }

      {
        WebElement assignedClientSelection = wait.until(d -> d.findElement(By.id("assigned-" + roleType)));
        List<WebElement> assignedOptions = assignedClientSelection.findElements(By.tagName("option"));
        addedRoles = assignedOptions.stream().map(WebElement::getText).collect(Collectors.toList());
      }

      directKeycloakAccessSetup.clearCache();
      Assertions.assertEquals(directKeycloakAccessSetup.getAllRealmRolesOfRealm(currentRealm).size(),
                              availableRoles.size());
      Assertions.assertEquals(0, addedRoles.size());
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("check removed " + roleType + " roles: " + resourceTypeName, () -> {
      new WaitStrategy().waitFor(() -> {
        directKeycloakAccessSetup.clearCache();
        List<ScimResourceTypeEntity> resourceTypes = directKeycloakAccessSetup.getResourceTypeEntities(currentRealm);
        ScimResourceTypeEntity resourceTypeEntity = resourceTypes.stream()
                                                                 .filter(rt -> rt.getName().equals(resourceTypeName))
                                                                 .findAny()
                                                                 .orElseThrow(IllegalStateException::new);
        Assertions.assertEquals(0, getRoleTypeEntities(roleType, resourceTypeEntity).size());
      });
    }));
    /* ******************************************************************************************************* */
    return dynamicTests;
  }

  /**
   * gets the correct role list of the given resource type for the given role type
   * 
   * @param roleType one of either [common, create, get, update, delete]
   * @param resourceTypeEntity the current resource type under test
   * @return the list of roles associated with the current resource type based on the given role type
   */
  private List<RoleEntity> getRoleTypeEntities(String roleType, ScimResourceTypeEntity resourceTypeEntity)
  {
    switch (roleType)
    {
      case "common":
        return resourceTypeEntity.getEndpointRoles();
      case "create":
        return resourceTypeEntity.getCreateRoles();
      case "get":
        return resourceTypeEntity.getGetRoles();
      case "update":
        return resourceTypeEntity.getUpdateRoles();
      case "delete":
        return resourceTypeEntity.getDeleteRoles();
      default:
        throw new IllegalArgumentException();
    }
  }
}
