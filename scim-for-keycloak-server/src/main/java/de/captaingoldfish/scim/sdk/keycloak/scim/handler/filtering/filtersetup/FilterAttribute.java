package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 14.12.2022
 */
@RequiredArgsConstructor
public class FilterAttribute
{

  @Getter
  private final String fullScimAttributeName;

  @Getter
  private final String jpqlMapping;

}
