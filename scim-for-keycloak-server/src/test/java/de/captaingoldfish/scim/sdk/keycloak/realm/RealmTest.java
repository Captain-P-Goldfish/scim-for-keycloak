package de.captaingoldfish.scim.sdk.keycloak.realm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractScimEndpointTest;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;


/**
 * @author Pascal Knueppel
 * @since 20.12.2022
 */
public class RealmTest extends AbstractScimEndpointTest
{

  /**
   * this test will show if a realm can be deleted that has users with scim created
   */
  @Test
  public void testDeleteRealmWithScimUsers()
  {
    CustomUser linkScim = JsonHelper.loadJsonDocument(USER_LINK, CustomUser.class);
    createUser(linkScim);
    Assertions.assertDoesNotThrow(() -> getKeycloakSession().realms().removeRealm(getRealmModel().getId()));
  }
}
