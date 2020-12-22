package de.captaingoldfish.scim.sdk.keycloak.custom.resources;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import lombok.Builder;


/**
 * a keycloak role as SCIM representation
 *
 * @author Pascal Knueppel
 * @since 16.08.2020
 */
public class RealmRole extends ResourceNode
{

  /**
   * the name of this resource
   */
  public static final String RESOURCE_NAME = "RealmRole";

  public RealmRole()
  {
    setSchemas(Collections.singletonList(FieldNames.SCHEMA));
  }

  @Builder
  public RealmRole(String id,
                   String externalId,
                   String name,
                   String description,
                   List<RoleAssociate> associates,
                   List<ChildRole> children,
                   Meta meta)
  {
    this();
    setId(id);
    setExternalId(externalId);
    setName(name);
    setDescription(description);
    setAssociates(associates);
    setChildren(children);
    setMeta(meta);
  }

  /**
   * the name of the keycloak role
   */
  public String getName()
  {
    return getStringAttribute(FieldNames.NAME).orElseThrow(() -> {
      return new IllegalStateException("missing required attribute");
    });
  }

  /**
   * the name of the keycloak role
   */
  public void setName(String name)
  {
    setAttribute(FieldNames.NAME, name);
  }

  /**
   * the description of the keycloak role
   */
  public Optional<String> getDescription()
  {
    return getStringAttribute(FieldNames.DESCRIPTION);
  }

  /**
   * the description of the keycloak role
   */
  public void setDescription(String description)
  {
    setAttribute(FieldNames.DESCRIPTION, description);
  }

  /**
   * the resources that are associated with the keycloak role
   */
  public List<RoleAssociate> getAssociates()
  {
    return getArrayAttribute(FieldNames.ASSOCIATES, RoleAssociate.class);
  }

  /**
   * the resources that are associated with the keycloak role
   */
  public void setAssociates(List<RoleAssociate> associates)
  {
    setAttribute(FieldNames.ASSOCIATES, associates);
  }

  /**
   * the resources that are associated with the keycloak role
   */
  public List<ChildRole> getChildren()
  {
    return getArrayAttribute(FieldNames.CHILDREN, ChildRole.class);
  }

  /**
   * the resources that are associated with the keycloak role
   */
  public void setChildren(List<ChildRole> children)
  {
    setAttribute(FieldNames.CHILDREN, children);
  }

  /**
   * contains simply the attribute names for the role representation
   */
  private static class FieldNames
  {

    public static final String SCHEMA = "urn:gold:params:scim:schemas:keycloak:2.0:RealmRole";

    public static final String NAME = "name";

    public static final String DESCRIPTION = "description";

    public static final String ASSOCIATES = "associates";

    public static final String CHILDREN = "children";
  }
}
