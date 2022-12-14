package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 14.12.2022
 */
@Getter
@RequiredArgsConstructor
public class TableJoin
{

  private final JpqlTableShortcuts baseTable;

  private final JpqlTableShortcuts joinTable;
}
