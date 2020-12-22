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
import org.keycloak.models.jpa.entities.ClientEntity;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

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
public class ServiceProviderAuthorizationTestBuilder extends AbstractTestBuilder
{

  /**
   * necessary to select the {@link ScimServiceProviderEntity} from the database
   */
  private final String currentRealm;

  public ServiceProviderAuthorizationTestBuilder(WebDriver webDriver,
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

    List<String> assignedClientIds = new ArrayList<>();
    Random random = new Random();
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("load service provider authorization menu", () -> {
      WebElement serviceProviderTab = untilClickable(By.id("service-provider-authorization-tab"));
      serviceProviderTab.click();
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("add clients randomly", () -> {
      WebElement availableClientSelection = wait.ignoring(StaleElementReferenceException.class)
                                                .until(d -> d.findElement(By.id("available")));
      Select select = new Select(availableClientSelection);
      List<WebElement> options = availableClientSelection.findElements(By.tagName("option"));
      for ( int i = 0 ; i < options.size() / 2 ; i++ )
      {
        final int index = random.nextInt(options.size());
        WebElement clientOption = options.get(index);
        String clientId = clientOption.getText();
        select.selectByValue(clientId);
        options.remove(index);
        assignedClientIds.add(clientId);
      }

      WebElement addSelectedButton = untilClickable(By.id("add-selected"));
      addSelectedButton.click();
      wait.ignoring(StaleElementReferenceException.class).until(d -> d.findElement(By.id("available")));
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("verify select lists are displaying elements correctly", () -> {
      List<String> availableClientIds;
      List<String> addedClientIds;

      {
        WebElement availableClientSelection = wait.until(d -> d.findElement(By.id("available")));
        List<WebElement> availableOptions = availableClientSelection.findElements(By.tagName("option"));
        availableClientIds = availableOptions.stream().map(WebElement::getText).collect(Collectors.toList());
      }

      {
        WebElement assignedClientSelection = wait.until(d -> d.findElement(By.id("assigned")));
        List<WebElement> assignedOptions = assignedClientSelection.findElements(By.tagName("option"));
        addedClientIds = assignedOptions.stream().map(WebElement::getText).collect(Collectors.toList());
      }

      MatcherAssert.assertThat(availableClientIds,
                               Matchers.not(Matchers.hasItems(assignedClientIds.stream()
                                                                               .map(Matchers::equalTo)
                                                                               .collect(Collectors.toList())
                                                                               .toArray(new Matcher[0]))));
      MatcherAssert.assertThat(addedClientIds,
                               Matchers.hasItems(assignedClientIds.stream()
                                                                  .map(Matchers::equalTo)
                                                                  .collect(Collectors.toList())
                                                                  .toArray(new Matcher[0])));
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("check assigned clients on database", () -> {
      new WaitStrategy().waitFor(() -> {
        directKeycloakAccessSetup.clearCache();
        ScimServiceProviderEntity serviceProviderEntity = directKeycloakAccessSetup.getServiceProviderEntity(currentRealm);
        List<String> assignedClients = serviceProviderEntity.getAuthorizedClients()
                                                            .stream()
                                                            .map(ClientEntity::getClientId)
                                                            .collect(Collectors.toList());
        MatcherAssert.assertThat(assignedClients,
                                 Matchers.containsInAnyOrder(assignedClientIds.stream()
                                                                              .map(Matchers::equalTo)
                                                                              .collect(Collectors.toList())));
      });
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("remove assigned clients from service provider", () -> {
      WebElement assignedClientSelection = wait.ignoring(StaleElementReferenceException.class)
                                               .until(d -> d.findElement(By.id("assigned")));
      Select select = new Select(assignedClientSelection);
      List<WebElement> options = assignedClientSelection.findElements(By.tagName("option"));
      for ( int i = 0 ; i < options.size() ; i++ )
      {
        select.selectByIndex(i);
      }
      WebElement removeSelectedButton = untilClickable(By.id("remove-selected"));
      removeSelectedButton.click();
      wait.ignoring(StaleElementReferenceException.class).until(d -> d.findElement(By.id("available")));
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("verify select lists are displaying elements correctly", () -> {
      List<String> availableClientIds;
      List<String> addedClientIds;

      {
        WebElement availableClientSelection = wait.until(d -> d.findElement(By.id("available")));
        List<WebElement> availableOptions = availableClientSelection.findElements(By.tagName("option"));
        availableClientIds = availableOptions.stream().map(WebElement::getText).collect(Collectors.toList());
      }

      {
        WebElement assignedClientSelection = wait.until(d -> d.findElement(By.id("assigned")));
        List<WebElement> assignedOptions = assignedClientSelection.findElements(By.tagName("option"));
        addedClientIds = assignedOptions.stream().map(WebElement::getText).collect(Collectors.toList());
      }

      directKeycloakAccessSetup.clearCache();
      Assertions.assertEquals(directKeycloakAccessSetup.getAllClientsOfRealm(currentRealm).size(),
                              availableClientIds.size());
      Assertions.assertEquals(0, addedClientIds.size());
    }));
    /* ******************************************************************************************************* */
    dynamicTests.add(DynamicTest.dynamicTest("check removed clients on database", () -> {
      new WaitStrategy().waitFor(() -> {
        directKeycloakAccessSetup.clearCache();
        ScimServiceProviderEntity serviceProviderEntity = directKeycloakAccessSetup.getServiceProviderEntity(currentRealm);
        List<ClientEntity> assignedClients = serviceProviderEntity.getAuthorizedClients();
        Assertions.assertEquals(0, assignedClients.size());
      });

    }));
    /* ******************************************************************************************************* */
    return dynamicTests;
  }
}
