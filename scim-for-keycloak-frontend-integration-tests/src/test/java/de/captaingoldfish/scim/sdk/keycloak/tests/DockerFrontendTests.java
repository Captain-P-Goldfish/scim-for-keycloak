package de.captaingoldfish.scim.sdk.keycloak.tests;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.DockerComposition;


/**
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
public class DockerFrontendTests extends FrontendTests
{

  public DockerFrontendTests()
  {
    super(new DockerComposition());
  }
}
