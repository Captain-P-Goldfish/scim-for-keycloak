package de.captaingoldfish.scim.sdk.keycloak.setup;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;

import lombok.SneakyThrows;


/**
 * @author Pascal Knueppel
 * @since 11.12.2022
 */
public interface FileReferences
{

  public static final String BASE_PATH = "/de/captaingoldfish/scim/sdk/files";

  public static final String USER_SUPER_MARIO = BASE_PATH + "/users/super-mario.json";

  public static final String USER_DONKEY_KONG = BASE_PATH + "/users/donkey-kong.json";

  public static final String USER_LINK = BASE_PATH + "/users/link.json";


  /**
   * reads a file from the test-resources and modifies the content
   *
   * @param resourcePath the path to the resource
   * @return the resource read into a string value
   */
  default String readResourceFile(String resourcePath)
  {
    return readResourceFile(resourcePath, null);
  }

  /**
   * reads a file from the test-resources and modifies the content
   *
   * @param resourcePath the path to the resource
   * @param changeResourceFileContent a function on the file content to modify the return string
   * @return the resource read into a string value
   */
  default String readResourceFile(String resourcePath, Function<String, String> changeResourceFileContent)
  {
    try (InputStream resourceInputStream = getClass().getResourceAsStream(resourcePath))
    {
      String content = IOUtils.toString(resourceInputStream, StandardCharsets.UTF_8.name());
      if (changeResourceFileContent != null)
      {
        content = changeResourceFileContent.apply(content);
      }
      return content;
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  /**
   * helper method to hide the required try-catch-block
   */
  @SneakyThrows
  default String encodeUrl(String url)
  {
    return URLEncoder.encode(url, StandardCharsets.UTF_8.name());
  }
}
