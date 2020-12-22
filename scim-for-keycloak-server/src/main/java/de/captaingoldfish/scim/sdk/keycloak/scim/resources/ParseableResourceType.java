package de.captaingoldfish.scim.sdk.keycloak.scim.resources;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidResourceTypeException;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;


/**
 * @author Pascal Knueppel
 * @since 06.08.2020
 */
public class ParseableResourceType extends ScimObjectNode
{

  /**
   * The resource type name. When applicable, service providers MUST specify the name, e.g., "User" or "Group".
   * This name is referenced by the "meta.resourceType" attribute in all resources. REQUIRED.
   */
  public String getName()
  {
    return getStringAttribute(AttributeNames.RFC7643.NAME).orElseThrow(() -> {
      return new InvalidResourceTypeException("the name is a required attribute", null, null, null);
    });
  }

  /**
   * The resource type name. When applicable, service providers MUST specify the name, e.g., "User" or "Group".
   * This name is referenced by the "meta.resourceType" attribute in all resources. REQUIRED.
   */
  public void setName(String name)
  {
    if (StringUtils.isBlank(name))
    {
      throw new InvalidResourceTypeException("the name is a required attribute", null, null, null);
    }
    setAttribute(AttributeNames.RFC7643.NAME, name);
  }

  /**
   * The resource type's human-readable description. When applicable, service providers MUST specify the
   * description. OPTIONAL.
   */
  public Optional<String> getDescription()
  {
    return getStringAttribute(AttributeNames.RFC7643.DESCRIPTION);
  }

  /**
   * The resource type's human-readable description. When applicable, service providers MUST specify the
   * description. OPTIONAL.
   */
  public void setDescription(String description)
  {
    setAttribute(AttributeNames.RFC7643.DESCRIPTION, description);
  }

  /**
   * @see ResourceTypeFeatures
   */
  public ResourceTypeFeatures getFeatures()
  {
    ResourceTypeFeatures filterExtension = getObjectAttribute(SchemaUris.RESOURCE_TYPE_FEATURE_EXTENSION_URI,
                                                              ResourceTypeFeatures.class).orElse(null);
    if (filterExtension == null)
    {
      filterExtension = ResourceTypeFeatures.builder().autoFiltering(false).singletonEndpoint(false).build();
      setFeatures(filterExtension);
    }
    return filterExtension;
  }

  /**
   * @see ResourceTypeFeatures
   */
  public void setFeatures(ResourceTypeFeatures filterExtension)
  {
    setAttribute(SchemaUris.RESOURCE_TYPE_FEATURE_EXTENSION_URI, filterExtension);
  }

  /**
   * A complex attribute containing resource metadata. All "meta" sub-attributes are assigned by the service
   * provider (have a "mutability" of "readOnly"), and all of these sub-attributes have a "returned"
   * characteristic of "default". This attribute SHALL be ignored when provided by clients. "meta" contains the
   * following sub-attributes:
   */
  public Optional<Meta> getMeta()
  {
    return getObjectAttribute(AttributeNames.RFC7643.META, Meta.class);
  }
}
