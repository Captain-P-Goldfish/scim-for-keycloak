package de.captaingoldfish.scim.sdk.keycloak.tests.setup.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.openqa.selenium.TimeoutException;

import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 14.12.2020
 */
@Slf4j
public final class WaitStrategy
{

  private final int timeoutInMillis;

  public WaitStrategy()
  {
    this(null);
  }

  public WaitStrategy(Integer timeoutInMillis)
  {
    this.timeoutInMillis = Optional.ofNullable(timeoutInMillis).orElse(5000);
  }

  public void waitFor(Runnable runnable)
  {
    Instant startTime = Instant.now();
    long timeDiff;
    boolean success = false;
    Throwable lastException = null;
    do
    {
      try
      {
        runnable.run();
        success = true;
      }
      catch (Exception | AssertionError ex)
      {
        log.debug(ex.getMessage(), ex);
        lastException = ex;
      }
      Instant now = Instant.now();
      timeDiff = Duration.between(startTime, now).toMillis();
    }
    while (!success && timeDiff <= timeoutInMillis);
    if (timeDiff > timeoutInMillis)
    {
      throw new TimeoutException("timed out while trying to retrieve data", lastException);
    }
  }

}
