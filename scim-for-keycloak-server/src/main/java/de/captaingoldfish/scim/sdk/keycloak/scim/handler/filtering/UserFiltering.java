package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.UserEntity;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.AbstractFiltering;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpqlTableShortcuts;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import lombok.extern.slf4j.Slf4j;


/**
 * this class implements the necessary details to filter users on database level
 * 
 * @author Pascal Knueppel
 * @since 11.12.2022
 */
@Slf4j
public class UserFiltering extends AbstractFiltering<ScimUserAttributesEntity>
{

  public UserFiltering(KeycloakSession keycloakSession,
                       long startIndex,
                       int count,
                       FilterNode filterNode,
                       SchemaAttribute sortBy,
                       SortOrder sortOrder)
  {
    super(keycloakSession, new UserAttributeMapping(), startIndex, count, filterNode, sortBy, sortOrder);

    log.info("initiate user filtering: startIndex: {}, count: {}, filter: {}, sortBy: {}, sortOrder: {}",
             startIndex,
             count,
             Optional.ofNullable(filterNode).map(FilterNode::toString).orElse(null),
             Optional.ofNullable(sortBy).map(SchemaAttribute::getName).orElse(null),
             Optional.ofNullable(sortOrder).map(Enum::name).orElse(null));
  }

  /**
   * builds the base JPQL query to filter users within the database. This query will look like this: as count
   * resource query:
   * 
   * <pre>
   *   select distinct count(u) from ScimUserAttributesEntity ua
   *   right join ua.userEntity u on u.id = ua.userEntity.id
   * </pre>
   * 
   * as resource query:
   * 
   * <pre>
   *   select distinct ua, u from ScimUserAttributesEntity ua
   *   right join ua.userEntity u on u.id = ua.userEntity.id
   * </pre>
   * 
   * @param countResources if the query should be a count query or a resource query
   * @return the basic query
   */
  @Override
  protected String getBaseQuery(boolean countResources)
  {
    final JpqlTableShortcuts jpqlUserEntity = JpqlTableShortcuts.USER_ENTITY;
    final JpqlTableShortcuts jpqlUserAttributes = JpqlTableShortcuts.SCIM_USER_ATTRIBUTES;

    String selectionString = countResources ? String.format("count(%s)", jpqlUserAttributes.getIdentifier())
      : String.format("%s, %s", jpqlUserAttributes.getIdentifier(), jpqlUserEntity.getIdentifier());

    return String.format("select distinct %1$s from %2$s %3$s " + "right join %3$s.%4$s %5$s on %5$s.id = %3$s.%4$s.id",
                         selectionString, // %1$s - either count expression or simple select expression
                         jpqlUserAttributes.getTableName(), // %2$s - ScimUserAttributesEntity
                         jpqlUserAttributes.getIdentifier(), // %3$s - ua
                         jpqlUserAttributes.getParentReference(), // %4$s - userEntity
                         jpqlUserEntity.getIdentifier()); // %5$s - ue
  }

  /**
   * takes the result stream from the database and parses the values into an appropriate object
   * 
   * @param resultStream the result stream from the database
   * @return the list of read users
   */
  @Override
  protected List<ScimUserAttributesEntity> parseResultStream(Stream<Object[]> resultStream)
  {
    return resultStream.map(objectArray -> {
      ScimUserAttributesEntity userAttributes = (ScimUserAttributesEntity)objectArray[0];
      if (userAttributes != null)
      {
        return userAttributes;
      }
      UserEntity userEntity = (UserEntity)objectArray[1];
      return ScimUserAttributesEntity.builder().userEntity(userEntity).build();
    }).collect(Collectors.toList());
  }

  /**
   * the where part that filters for current realm and users that are not of type service-account. This part of
   * the where clause will look like this:
   *
   * <pre>
   *       u.realmId = '58e42084-f249-4866-a3c3-b9b9535da5a3'
   *   and u.serviceAccountClientLink is null
   * </pre>
   */
  @Override
  protected String getRealmRestrictionClause()
  {
    final String tableShortcut = JpqlTableShortcuts.USER_ENTITY.getIdentifier();
    return String.format("%s.realmId = '%s' and %s.serviceAccountClientLink is null",
                         tableShortcut,
                         realmModel.getId(),
                         tableShortcut);
  }
}
