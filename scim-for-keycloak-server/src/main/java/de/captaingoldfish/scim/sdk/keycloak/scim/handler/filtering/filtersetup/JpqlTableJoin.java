package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup;

import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * this class represents joins that must be executed within the jpql query string
 * 
 * @author Pascal Knueppel
 * @since 14.12.2022
 */
@EqualsAndHashCode
@RequiredArgsConstructor
public class JpqlTableJoin
{

  /**
   * the table that is used join-base
   */
  @Getter
  private final JpaEntityReferences baseTable;

  /**
   * the table onto which will be joined
   */
  @Getter
  private final JpaEntityReferences joinTable;

  /**
   * the name of the entities member-attribute that is used in the JPQL query e.g. "id" for
   * {@link org.keycloak.models.jpa.entities.UserEntity}
   */
  private final String joinOnBase;

  /**
   * the fully qualified attributes name that is used for joining onto the base-table e.g. "userEntity.id" for
   * {@link de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity}
   */
  private final String joinOn;

  /**
   * if this join should appear within the JPQL selection clause or if its to be used for the where-clause only
   */
  @Getter
  private final boolean joinIntoSelection;

  public JpqlTableJoin(JpaEntityReferences baseTable)
  {
    this.baseTable = baseTable;
    this.joinTable = null;
    this.joinOnBase = null;
    this.joinOn = null;
    this.joinIntoSelection = false;
  }

  /**
   * builds the join-jpql statement that results from this object
   */
  public String getJoinJpql()
  {
    if (joinTable == null)
    {
      return "";
    }

    /*
     * produces the on-expression for the necessary inner join e.g.:
     * @formatter:off
     *      u.id = ua.userEntity.id
     *        or
     *      ugm.groupId = g.id
     * @formatter:on
     */
    final String onExpression = String.format("%s = %s", getJoinOnBase(), getJoinOn());

    /*
     * produces the complete-expression for the necessary inner join e.g.:
     * @formatter:off
     *      join ScimUserAttributesEntity ua on u.id = ua.userEntity.id
     *        or
     *      join GroupEntity on ugm.groupId = g.id
     * @formatter:on
     */
    return String.format("join %s %s on %s", joinTable.getTableName(), joinTable.getIdentifier(), onExpression);
  }

  /**
   * the fully qualified base-join-attribute for the JPQL statement
   */
  private String getJoinOnBase()
  {
    return String.format("%s.%s", baseTable.getIdentifier(), joinOnBase);
  }

  /**
   * the fully qualified join-attribute for the JPQL statement
   */
  private String getJoinOn()
  {
    if (joinTable == null)
    {
      return null;
    }
    return String.format("%s.%s", joinTable.getIdentifier(), joinOn);
  }

  @Override
  public String toString()
  {
    return String.format("[from '%s' join '%s']",
                         baseTable.getTableName(),
                         Optional.ofNullable(joinTable).map(JpaEntityReferences::getTableName).orElse(""));
  }
}
