package de.captaingoldfish.scim.sdk.keycloak.custom.resources;

import de.captaingoldfish.scim.sdk.common.resources.multicomplex.MultiComplexNode;
import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 17.08.2020
 */
@NoArgsConstructor
public class RoleAssociate extends MultiComplexNode
{

  @Builder
  public RoleAssociate(String type, String display, String value)
  {
    super(type, null, display, value, null);
  }
}
