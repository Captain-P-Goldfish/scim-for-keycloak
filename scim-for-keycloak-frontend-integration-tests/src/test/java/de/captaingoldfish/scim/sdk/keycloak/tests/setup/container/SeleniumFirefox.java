package de.captaingoldfish.scim.sdk.keycloak.tests.setup.container;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;

import lombok.extern.slf4j.Slf4j;


/**
 * this class is used to start a selenium container with a firefox web driver
 * 
 * @author Pascal Knüppel
 * @since 12.12.2020
 */
@Slf4j
public class SeleniumFirefox
{

  /**
   * the selenium docker container instance
   */
  private BrowserWebDriverContainer firefox;

  public SeleniumFirefox(Network network)
  {
    firefox = new BrowserWebDriverContainer().withCapabilities(new FirefoxOptions().setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.ACCEPT_AND_NOTIFY));
    firefox.withNetwork(network);
  }

  /**
   * starts the selenium docker container
   */
  public void start()
  {
    firefox.start();
    configure();
  }

  /**
   * stops the selenium docker container
   */
  public void stop()
  {
    try
    {
      firefox.getWebDriver().close();
    }
    catch (Exception ex)
    {
      log.warn("could not close selenium web driver properly", ex);
    }
    try
    {
      firefox.stop();
    }
    catch (Exception ex)
    {
      log.warn("could not close selenium docker container properly", ex);
    }
  }

  /**
   * @return a selenium web driver instance
   */
  public RemoteWebDriver getDriver()
  {
    return firefox.getWebDriver();
  }

  /**
   * configures the the selenium web driver
   */
  private void configure()
  {
    RemoteWebDriver webDriver = getDriver();
    webDriver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
    webDriver.manage().timeouts().pageLoadTimeout(5, TimeUnit.SECONDS);
    webDriver.manage().timeouts().setScriptTimeout(5, TimeUnit.SECONDS);
  }
}
