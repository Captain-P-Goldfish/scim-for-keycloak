package de.captaingoldfish.scim.sdk.keycloak.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 04.02.2020 <br>
 * <br>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContextPaths
{

  /**
   * the base path for all SCIM end points
   */
  public static final String SCIM_BASE_PATH = "scim";

  /**
   * the path to the {@link de.captaingoldfish.scim.sdk.keycloak.scim.ScimEndpoint} class <br/>
   * note that by default the base {@link #SCIM_BASE_PATH} is prepended to this path since it has been used as
   * provider factory id
   */
  public static final String SCIM_ENDPOINT_PATH = "/v2";

  /**
   * context path to the SCIM administration
   */
  public static final String ADMIN = "/admin";
}
