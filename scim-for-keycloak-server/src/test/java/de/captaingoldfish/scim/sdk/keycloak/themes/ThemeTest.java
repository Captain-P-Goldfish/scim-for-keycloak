package de.captaingoldfish.scim.sdk.keycloak.themes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.encoders.Hex;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import lombok.SneakyThrows;


/**
 * @author Pascal Knueppel
 * @since 18.12.2020
 */
public class ThemeTest
{

  /**
   * this unit test is explicitly added for updating to new keycloak versions. It will inform me as soon as
   * there are changes in the kc-menu.html file
   */
  @SneakyThrows
  @Test
  public void testKcMenuIsUnchanged()
  {
    final String hashOfKcMenu = "965db92676daeae3925adcb0ada3858c1075fdbb";
    MessageDigest sha1DigestBuilder = MessageDigest.getInstance("SHA-1");
    final String kcMenuLocation = "/theme/base/admin/resources/templates/kc-menu.html";
    byte[] kcMenuBytes = IOUtils.toByteArray(getClass().getResourceAsStream(kcMenuLocation));
    MatcherAssert.assertThat("did not find kc-menu.html", kcMenuBytes.length, Matchers.greaterThan(0));
    byte[] kcMenuDigest = Hex.encode(sha1DigestBuilder.digest(kcMenuBytes));
    Assertions.assertArrayEquals(hashOfKcMenu.getBytes(StandardCharsets.UTF_8),
                                 kcMenuDigest,
                                 String.format("expected hash: '%s'\nactual hash: '%s'",
                                               hashOfKcMenu,
                                               new String(kcMenuDigest)));
  }
}
