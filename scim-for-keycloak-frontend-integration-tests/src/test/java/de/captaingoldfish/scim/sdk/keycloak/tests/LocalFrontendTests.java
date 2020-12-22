package de.captaingoldfish.scim.sdk.keycloak.tests;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.LocalComposition;


/**
 * execute frontend tests with a local already running keycloak setup. (the local tests are simply for *
 * increasing development performance)
 * 
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
@Disabled("for local setup use only")
public class LocalFrontendTests extends FrontendTests
{

  public LocalFrontendTests()
  {
    super(new LocalComposition());
  }

  @BeforeAll
  public static void initSelenium()
  {
    System.setProperty("webdriver.gecko.driver", new File(".").getAbsolutePath() + "/geckodriver.exe");
  }

}
