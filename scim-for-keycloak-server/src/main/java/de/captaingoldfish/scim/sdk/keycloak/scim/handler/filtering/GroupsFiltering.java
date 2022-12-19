package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Query;

import org.apache.commons.lang3.tuple.Triple;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.GroupEntity;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.AbstractFiltering;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpqlTableJoin;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 19.12.2022
 */
@Slf4j
public class GroupsFiltering extends AbstractFiltering<GroupEntity>
{


  public GroupsFiltering(KeycloakSession keycloakSession,
                         long startIndex,
                         int count,
                         FilterNode filterNode,
                         SchemaAttribute sortBy,
                         SortOrder sortOrder)
  {
    super(keycloakSession, GroupAttributeMapping.getInstance(), startIndex, count, filterNode, sortBy, sortOrder);
  }

  /**
   * the base selection to select from the group table
   */
  @Override
  protected JpqlTableJoin getBaseSelection()
  {
    return new JpqlTableJoin(JpaEntityReferences.GROUPS_ENTITY);
  }

  /**
   * restricts the filter to the current realm
   */
  @Override
  protected String getRealmRestrictionClause()
  {
    final String tableShortcut = JpaEntityReferences.GROUPS_ENTITY.getIdentifier();
    return String.format("%s.realm = '%s'", tableShortcut, realmModel.getId());
  }

  /**
   * retireves the read {@link GroupEntity}s from the jpql filter
   */
  @Override
  protected List<GroupEntity> parseResultStream(Stream<Object> resultStream)
  {
    return resultStream.map(o -> (GroupEntity)o).collect(Collectors.toList());
  }

  /**
   * gets the members of the groups with the given ids
   * 
   * @param groupIds the ids of the groups for which the members should be retrieved
   * @return a list of triples that are composed of {@code <groupId, userId, username>}
   */
  public List<Triple<String, String, String>> getGroupMembers(List<String> groupIds)
  {
    final String jpqlQuery = "select distinct g.id, u.id, u.username from GroupEntity g "
                             + "inner join UserGroupMembershipEntity ugm on g.id = ugm.groupId "
                             + "inner join UserEntity u on ugm.user.id = u.id where g.id in (:groupIdList) "
                             + "order by g.id";

    log.debug("Loading group-members with query: {}", jpqlQuery);

    Query query = getEntityManager().createQuery(jpqlQuery);
    query.setParameter("groupIdList", groupIds);

    Stream<Object[]> resultStream = query.getResultStream();
    return resultStream.map(objectArray -> {
      return Triple.of((String)objectArray[0], (String)objectArray[1], (String)objectArray[2]);
    }).collect(Collectors.toList());
  }
}
