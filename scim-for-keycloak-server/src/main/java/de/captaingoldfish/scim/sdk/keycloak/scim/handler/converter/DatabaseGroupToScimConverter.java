package de.captaingoldfish.scim.sdk.keycloak.scim.handler.converter;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;
import org.keycloak.models.jpa.entities.GroupAttributeEntity;
import org.keycloak.models.jpa.entities.GroupEntity;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames.RFC7643;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.utils.TimeUtils;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.GroupsFiltering;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 19.12.2022
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DatabaseGroupToScimConverter
{

  /**
   * parses a database group representation into its SCIM representation
   */
  public static Group databaseGroupModelToScimModel(GroupEntity groupEntity)
  {
    Map<String, String> wantedAttributeMap = getAttributesByNames(groupEntity,
                                                                  Arrays.asList(RFC7643.CREATED,
                                                                                RFC7643.LAST_MODIFIED));
    Instant created = Optional.ofNullable(wantedAttributeMap.get(RFC7643.CREATED))
                              .map(TimeUtils::parseDateTime)
                              .orElse(Instant.now());
    Instant lastModified = Optional.ofNullable(wantedAttributeMap.get(RFC7643.LAST_MODIFIED))
                                   .map(TimeUtils::parseDateTime)
                                   .orElse(Instant.now());
    return Group.builder()
                .id(groupEntity.getId())
                .displayName(groupEntity.getName())
                .meta(Meta.builder().created(created).lastModified(lastModified).build())
                .build();
  }

  /**
   * retrieves a specific attribute from the given group by its name
   * 
   * @param groupEntity the entity that contains some attributes
   * @param attributeNameList the list of attributes to retrieve
   * @return the attributes as key, value map that were requested from the group instance
   */
  private static Map<String, String> getAttributesByNames(GroupEntity groupEntity, List<String> attributeNameList)
  {
    return groupEntity.getAttributes()
                      .parallelStream()
                      .filter(attribute -> attributeNameList.contains(attribute.getName()))
                      .collect(Collectors.toMap(GroupAttributeEntity::getName, GroupAttributeEntity::getValue));
  }

  /**
   * filters all existing group members and adds them to the corresponding groups within the groups list
   * 
   * @param groupsFiltering used to find the group-members
   * @param groups the groups for which the members should be found
   */
  public static void addMembersToGroups(GroupsFiltering groupsFiltering, List<Group> groups)
  {
    List<String> groupIds = groups.parallelStream().map(group -> group.getId().get()).collect(Collectors.toList());
    if (groupIds.isEmpty())
    {
      return;
    }
    List<Triple<String, String, String>> groupMembersTriple = groupsFiltering.getGroupMembers(groupIds);
    // @formatter:off
    Map<String, List<Triple<String, String, String>>> membershipMap = 
                      groupMembersTriple.parallelStream().collect(Collectors.groupingBy(Triple::getLeft));
    // @formatter:on
    membershipMap.entrySet().parallelStream().forEach(membership -> {
      Group group = groups.stream().filter(g -> g.getId().get().equals(membership.getKey())).findAny().get();
      List<Member> members = membership.getValue().parallelStream().map(triple -> {
        final String userId = triple.getMiddle();
        final String username = triple.getRight();
        return Member.builder().value(userId).display(username).type(ResourceTypeNames.USER).build();
      }).collect(Collectors.toList());
      group.setMembers(members);
    });
  }

}
