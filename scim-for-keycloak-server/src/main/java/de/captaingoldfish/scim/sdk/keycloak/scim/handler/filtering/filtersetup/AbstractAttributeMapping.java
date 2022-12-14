package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;


/**
 * @author Pascal Knueppel
 * @since 12.12.2022
 */
public abstract class AbstractAttributeMapping
{

  /**
   * the attributes that will be used to map from SCIM definition to database definition e.g.
   *
   * <pre>
   *   name.givenName -> {@link ScimUserAttributesEntity#getGivenName()}
   * </pre>
   * 
   * these attribute-mapping is used to build a JPQL query from the SCIM filter expression
   */
  private final Map<String, FilterAttribute> attributeMapping = new HashMap<>();

  /**
   * will add a SCIM attribute with its mapping to the JPA entity attribute
   * 
   * @param resourceUri the resource uri of the SCIM attribute. This is the attributes full schema-uri
   * @param complexParentName optional param. If the parameter has a parent attribute this will be the parents
   *          name e .g. "email" or "name"
   * @param simpleAttributeName the child attributes name e.g. "value" or "givenName"
   * @param jpqlShortcut the shortcut that will be used within JPQL queries to identify the entity to which the
   *          attribute belongs
   * @param jpqlReference the attributes name within the jpa entity. This attribute must be a member of the
   *          reference within {@code jpqlShortcut}
   */
  protected void addAttribute(String resourceUri,
                              String complexParentName,
                              String simpleAttributeName,
                              JpqlTableShortcuts jpqlShortcut,
                              String jpqlReference)
  {
    addAttribute(resourceUri, complexParentName, simpleAttributeName, jpqlShortcut, jpqlReference, null);
  }

  /**
   * will add a SCIM attribute with its mapping to the JPA entity attribute
   * 
   * @param resourceUri the resource uri of the SCIM attribute. This is the attributes full schema-uri
   * @param complexParentName optional param. If the parameter has a parent attribute this will be the parents
   *          name e .g. "email" or "name"
   * @param simpleAttributeName the child attributes name e.g. "value" or "givenName"
   * @param jpqlShortcut the shortcut that will be used within JPQL queries to identify the entity to which the
   *          attribute belongs
   * @param jpqlReference the attributes name within the jpa entity. This attribute must be a member of the
   *          reference within {@code jpqlShortcut}
   */
  protected void addAttribute(String resourceUri,
                              String complexParentName,
                              String simpleAttributeName,
                              JpqlTableShortcuts jpqlShortcut,
                              String jpqlReference,
                              JpqlTableShortcuts joinTable)
  {
    final String attributeName = String.format("%s%s",
                                               Optional.ofNullable(complexParentName).map(s -> s + ".").orElse(""),
                                               simpleAttributeName);
    final String fullAttributeName = String.format("%s:%s", resourceUri, attributeName);

    final boolean isJoinRequired = joinTable != null;
    final String jpqlIdentifier = isJoinRequired ? joinTable.getIdentifier() : jpqlShortcut.getIdentifier();
    final String jpqlFullPath = String.format("%s.%s", jpqlIdentifier, jpqlReference);

    TableJoin tableJoin = isJoinRequired ? new TableJoin(jpqlShortcut, joinTable) : null;
    FilterAttribute filterAttribute = new FilterAttribute(fullAttributeName, jpqlFullPath, tableJoin);
    attributeMapping.put(fullAttributeName, filterAttribute);
  }

  /**
   * retrieves an attribute from the mapping table and throws an exception if the attribute was not found. This
   * might happen if someone tries to filter on illegal attributes like a users password
   * 
   * @param attributeName the name of the attribute which we want to map to JPQL
   * @return the JPQL attribute-reference. e.g. "ue.userName" if the select statement before was "select ue from
   *         UserEntity ue"
   */
  protected FilterAttribute getAttribute(String attributeName)
  {
    FilterAttribute attributeReference = attributeMapping.get(attributeName);
    if (attributeReference == null)
    {
      // will e.g. happen if someone tries to filter for passwords
      throw new BadRequestException(String.format("Illegal filter-attribute found '%s'", attributeName));
    }
    return attributeReference;
  }
}
