package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup;

import java.util.List;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 14.12.2022
 */
@RequiredArgsConstructor
public class FilterAttribute
{


  /**
   * the full scim attribute name. Just added for debugging purposes to see during debugging which scim
   * attribute is mapped onto this database-attribute.
   */
  @Getter
  private final String fullScimAttributeName;

  /**
   * the entities member-attribute which maps onto the database column. E.g. "username" which references
   * {@link org.keycloak.models.jpa.entities.UserEntity#username}
   */
  private final String databaseAttributeName;

  /**
   * the joins that are necessary to be executed in order to do a where-expression on this attribute. It is
   * necessary that no duplicate entries are added into this list. We cannot use a {@link java.util.Set} though
   * since we need to maintain the order of the joins.
   */
  @Getter
  private final List<JpqlTableJoin> joins;

  /**
   * the JPQL attribute mapping. This should look like this:
   * 
   * <pre>
   *    u.username         |-> user.username
   *    or                 |
   *    ua.externalId      |-> scimUserAttributes.externalId
   *    or                 |
   *    g.name             |-> group.name
   * </pre>
   */
  public String getJpqlMapping()
  {
    // the correct table reference is always the last one in the list
    JpqlTableJoin jpqlTableJoin = joins.get(joins.size() - 1);
    return String.format("%s.%s",
                         Optional.ofNullable(jpqlTableJoin.getJoinTable())
                                 .orElse(jpqlTableJoin.getBaseTable())
                                 .getIdentifier(),
                         databaseAttributeName);
  }

}
