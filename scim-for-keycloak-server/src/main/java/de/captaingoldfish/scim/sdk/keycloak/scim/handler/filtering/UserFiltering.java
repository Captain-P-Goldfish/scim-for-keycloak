package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering;

import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.SCIM_USER_ATTRIBUTES;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.USER_ENTITY;

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
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpqlTableJoin;
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
   * the jpa entity on which the select will be started. This entity might be joined with other entities.
   * 
   * @return the basic jpa entity to start with to do some sql
   */
  @Override
  protected JpqlTableJoin getBaseSelection()
  {
    return new JpqlTableJoin(USER_ENTITY, SCIM_USER_ATTRIBUTES, "id", "userEntity.id", true);
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
      UserEntity userEntity = (UserEntity)objectArray[0];
      if (objectArray.length == 1)
      {
        return ScimUserAttributesEntity.builder().userEntity(userEntity).build();
      }
      // since to the nature of the UserAttributesMapping we know that we get maximum 2 entities and exactly in this
      // order
      return (ScimUserAttributesEntity)objectArray[1];
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
    final String tableShortcut = JpaEntityReferences.USER_ENTITY.getIdentifier();
    return String.format("%s.realmId = '%s' and %s.serviceAccountClientLink is null",
                         tableShortcut,
                         realmModel.getId(),
                         tableShortcut);
  }
}
