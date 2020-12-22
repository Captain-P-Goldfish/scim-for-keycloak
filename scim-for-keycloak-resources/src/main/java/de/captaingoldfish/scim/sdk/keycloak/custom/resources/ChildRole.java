package de.captaingoldfish.scim.sdk.keycloak.custom.resources;

import de.captaingoldfish.scim.sdk.common.resources.multicomplex.MultiComplexNode;
import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 17.08.2020
 */
@NoArgsConstructor
public class ChildRole extends MultiComplexNode
{

  @Builder
  public ChildRole(String type, String display, String value, Boolean clientRole)
  {
    super(type, null, display, value, null);
    setClientRole(clientRole);
  }

  /**
   * if this child role is a client role or not
   */
  public boolean isClientRole()
  {
    return getBooleanAttribute(FieldNames.CLIENT_ROLE).orElse(false);
  }

  /**
   * if this child role is a client role or not
   */
  public void setClientRole(Boolean clientRole)
  {
    setAttribute(FieldNames.CLIENT_ROLE, clientRole);
  }

  private static class FieldNames
  {

    public static final String CLIENT_ROLE = "clientRole";
  }
}
