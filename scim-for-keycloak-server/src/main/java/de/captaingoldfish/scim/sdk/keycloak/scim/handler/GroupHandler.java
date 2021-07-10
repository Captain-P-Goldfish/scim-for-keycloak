package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.keycloak.audit.ScimAdminEventBuilder;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimKeycloakContext;
import de.captaingoldfish.scim.sdk.keycloak.services.GroupService;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 04.02.2020 <br>
 * <br>
 */
@Slf4j
public class GroupHandler extends ResourceHandler<Group>
{

  /**
   * {@inheritDoc}
   */
  @Override
  public Group createResource(Group group, Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    final String groupName = group.getDisplayName().get();
    if (new GroupService(keycloakSession).getGroupByName(groupName).isPresent())
    {
      throw new ConflictException("a group with name '" + groupName + "' does already exist");
    }
    GroupModel groupModel = keycloakSession.getContext().getRealm().createGroup(groupName);
    groupModel = groupToModel((ScimKeycloakContext)context, group, groupModel);
    Group newGroup = modelToGroup(keycloakSession, groupModel);
    {
      ScimAdminEventBuilder adminEventAuditer = ((ScimKeycloakContext)context).getAdminEventAuditer();
      adminEventAuditer.createEvent(OperationType.CREATE,
                                    ResourceType.GROUP,
                                    String.format("groups/%s", groupModel.getId()),
                                    newGroup);
    }
    log.debug("Created group with name: {}", groupModel.getName());
    return newGroup;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Group getResource(String id,
                           List<SchemaAttribute> attributes,
                           List<SchemaAttribute> excludedAttributes,
                           Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    GroupModel groupModel = keycloakSession.getContext().getRealm().getGroupById(id);
    if (groupModel == null)
    {
      return null; // causes a resource not found exception you may also throw it manually
    }
    return modelToGroup(keycloakSession, groupModel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PartialListResponse<Group> listResources(long startIndex,
                                                  int count,
                                                  FilterNode filter,
                                                  SchemaAttribute sortBy,
                                                  SortOrder sortOrder,
                                                  List<SchemaAttribute> attributes,
                                                  List<SchemaAttribute> excludedAttributes,
                                                  Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    // TODO in order to filter on database level the feature "autoFiltering" must be disabled and the JPA criteria
    // api should be used
    Stream<GroupModel> groupModelsStream = keycloakSession.getContext().getRealm().getGroupsStream();
    List<Group> groupList = groupModelsStream.map(groupModel -> modelToGroup(keycloakSession, groupModel))
                                             .collect(Collectors.toList());
    return PartialListResponse.<Group> builder()
                              .totalResults(keycloakSession.getContext().getRealm().getGroupsCount(false))
                              .resources(groupList)
                              .build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Group updateResource(Group groupToUpdate, Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    GroupModel groupModel = keycloakSession.getContext().getRealm().getGroupById(groupToUpdate.getId().get());
    if (groupModel == null)
    {
      return null; // causes a resource not found exception you may also throw it manually
    }
    groupModel = groupToModel((ScimKeycloakContext)context, groupToUpdate, groupModel);
    Group group = modelToGroup(keycloakSession, groupModel);
    {
      ScimAdminEventBuilder adminEventAuditer = ((ScimKeycloakContext)context).getAdminEventAuditer();
      adminEventAuditer.createEvent(OperationType.UPDATE,
                                    ResourceType.GROUP,
                                    String.format("groups/%s", groupModel.getId()),
                                    group);
    }
    log.debug("Updated group with name: {}", groupModel.getName());
    return group;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteResource(String id, Context context)
  {
    KeycloakSession keycloakSession = ((ScimKeycloakContext)context).getKeycloakSession();
    GroupModel groupModel = keycloakSession.getContext().getRealm().getGroupById(id);
    if (groupModel == null)
    {
      throw new ResourceNotFoundException("group with id '" + id + "' does not exist");
    }
    keycloakSession.getContext().getRealm().removeGroup(groupModel);
    {
      ScimAdminEventBuilder adminEventAuditer = ((ScimKeycloakContext)context).getAdminEventAuditer();
      adminEventAuditer.createEvent(OperationType.DELETE,
                                    ResourceType.GROUP,
                                    String.format("groups/%s", groupModel.getId()),
                                    Group.builder().id(groupModel.getId()).displayName(groupModel.getName()).build());
    }
    log.debug("Deleted group with name: {}", groupModel.getName());
  }

  /**
   * writes the group values into the keycloak group representation
   *
   * @param group the scim group representation
   * @param groupModel the keycloak group representation
   * @return the overridden keycloak group representation
   */
  private GroupModel groupToModel(ScimKeycloakContext scimKeycloakContext, Group group, GroupModel groupModel)
  {
    KeycloakSession keycloakSession = scimKeycloakContext.getKeycloakSession();
    RealmModel realmModel = keycloakSession.getContext().getRealm();
    group.getDisplayName().ifPresent(groupModel::setName);
    group.getExternalId()
         .ifPresent(externalId -> groupModel.setSingleAttribute(AttributeNames.RFC7643.EXTERNAL_ID, externalId));


    updateUserMemberships(scimKeycloakContext, group, groupModel, realmModel);
    updateGroupMemberships(scimKeycloakContext, group, groupModel, realmModel);

    return groupModel;
  }

  /**
   * remove groups that are no longer associated with the current group and adds the newly associated groups
   * 
   * @param group the scim group model as it must be after the change
   * @param groupModel the current group model
   */
  private void updateGroupMemberships(ScimKeycloakContext scimKeycloakContext,
                                      Group group,
                                      GroupModel groupModel,
                                      RealmModel realmModel)
  {
    KeycloakSession keycloakSession = scimKeycloakContext.getKeycloakSession();

    Set<String> expectedSubGroupMemberIds = group.getMembers()
                                                 .stream()
                                                 .filter(groupMember -> groupMember.getType().isPresent()
                                                                        && groupMember.getType()
                                                                                      .get()
                                                                                      .equalsIgnoreCase("Group"))
                                                 .map(groupMember -> groupMember.getValue().get())
                                                 .collect(Collectors.toSet());
    Set<GroupModel> subGroupsToLeaveGroup = groupModel.getSubGroupsStream().collect(Collectors.toSet());

    ScimAdminEventBuilder adminEventAuditer = scimKeycloakContext.getAdminEventAuditer();

    List<String> subGroupIdsToStayInGroup = new ArrayList<>();
    subGroupsToLeaveGroup.removeIf(gm -> {
      boolean doNotRemoveFromGroup = expectedSubGroupMemberIds.contains(gm.getId());
      if (doNotRemoveFromGroup)
      {
        subGroupIdsToStayInGroup.add(gm.getId());
      }
      return doNotRemoveFromGroup;
    });
    subGroupsToLeaveGroup.forEach(subGroup -> {
      groupModel.removeChild(subGroup);
      adminEventAuditer.createEvent(OperationType.DELETE,
                                    ResourceType.GROUP_MEMBERSHIP,
                                    String.format("groups/%s/groups/%s", subGroup.getId(), groupModel.getId()),
                                    Group.builder()
                                         .id(groupModel.getId())
                                         .displayName(groupModel.getName())
                                         .members(Collections.singletonList(Member.builder()
                                                                                  .type(ResourceTypeNames.GROUPS)
                                                                                  .display(subGroup.getName())
                                                                                  .value(subGroup.getId())
                                                                                  .build()))
                                         .build());
    });

    expectedSubGroupMemberIds.stream().filter(expectedSubGroupMemberId -> {
      return !subGroupIdsToStayInGroup.contains(expectedSubGroupMemberId);
    }).forEach(newSubGroupMemberId -> {
      GroupModel newMember = keycloakSession.groups().getGroupById(realmModel, newSubGroupMemberId);
      if (newMember == null)
      {
        throw new ResourceNotFoundException(String.format("Group with id '%s' does not exist", newSubGroupMemberId));
      }
      groupModel.addChild(newMember);
      adminEventAuditer.createEvent(OperationType.CREATE,
                                    ResourceType.GROUP_MEMBERSHIP,
                                    String.format("groups/%s/groups/%s", newMember.getId(), groupModel.getId()),
                                    Group.builder()
                                         .id(groupModel.getId())
                                         .displayName(groupModel.getName())
                                         .members(Collections.singletonList(Member.builder()
                                                                                  .type(ResourceTypeNames.GROUPS)
                                                                                  .display(newMember.getName())
                                                                                  .value(newMember.getId())
                                                                                  .build()))
                                         .build());
    });
  }

  /**
   * remove users that are no longer associated with the current group and adds the newly associated users
   * 
   * @param group the scim group model as it must be after the change
   * @param groupModel the current group model
   */
  private void updateUserMemberships(ScimKeycloakContext scimKeycloakContext,
                                     Group group,
                                     GroupModel groupModel,
                                     RealmModel realmModel)
  {
    KeycloakSession keycloakSession = scimKeycloakContext.getKeycloakSession();
    Set<String> expectedUserMemberIds = group.getMembers()
                                             .stream()
                                             .filter(groupMember -> groupMember.getType().isPresent()
                                                                    && groupMember.getType()
                                                                                  .get()
                                                                                  .equalsIgnoreCase("User"))
                                             .map(groupMember -> groupMember.getValue().get())
                                             .collect(Collectors.toSet());
    List<UserModel> usersToLeaveGroup = keycloakSession.users()
                                                       .getGroupMembersStream(realmModel, groupModel)
                                                       .collect(Collectors.toList());

    ScimAdminEventBuilder adminEventAuditer = scimKeycloakContext.getAdminEventAuditer();

    List<String> userIdsToStayInGroup = new ArrayList<>();
    usersToLeaveGroup.removeIf(userModel -> {
      boolean doNotRemoveFromGroup = expectedUserMemberIds.contains(userModel.getId());
      if (doNotRemoveFromGroup)
      {
        userIdsToStayInGroup.add(userModel.getId());
      }
      return doNotRemoveFromGroup;
    });
    usersToLeaveGroup.forEach(userModel -> {
      userModel.leaveGroup(groupModel);
      adminEventAuditer.createEvent(OperationType.DELETE,
                                    ResourceType.GROUP_MEMBERSHIP,
                                    String.format("users/%s/groups/%s", userModel.getId(), groupModel.getId()),
                                    Group.builder()
                                         .id(groupModel.getId())
                                         .displayName(groupModel.getName())
                                         .members(Collections.singletonList(Member.builder()
                                                                                  .type(ResourceTypeNames.USER)
                                                                                  .display(userModel.getUsername())
                                                                                  .value(userModel.getId())
                                                                                  .build()))
                                         .build());
    });

    expectedUserMemberIds.stream().filter(expectedUserMemberId -> {
      return !userIdsToStayInGroup.contains(expectedUserMemberId);
    }).forEach(newUserMemberId -> {
      UserModel newMember = keycloakSession.users().getUserById(realmModel, newUserMemberId);
      if (newMember == null)
      {
        throw new ResourceNotFoundException(String.format("User with id '%s' does not exist", newUserMemberId));
      }
      newMember.joinGroup(groupModel);
      adminEventAuditer.createEvent(OperationType.CREATE,
                                    ResourceType.GROUP_MEMBERSHIP,
                                    String.format("users/%s/groups/%s", newMember.getId(), groupModel.getId()),
                                    Group.builder()
                                         .id(groupModel.getId())
                                         .displayName(groupModel.getName())
                                         .members(Collections.singletonList(Member.builder()
                                                                                  .type(ResourceTypeNames.USER)
                                                                                  .display(newMember.getUsername())
                                                                                  .value(newMember.getId())
                                                                                  .build()))
                                         .build());
    });
  }

  /**
   * converts a keycloak {@link GroupModel} into a SCIM representation of {@link Group}
   *
   * @param groupModel the keycloak group representation
   * @return the SCIM group representation
   */
  private Group modelToGroup(KeycloakSession keycloakSession, GroupModel groupModel)
  {
    return Group.builder()
                .id(groupModel.getId())
                .externalId(groupModel.getFirstAttribute(AttributeNames.RFC7643.EXTERNAL_ID))
                .displayName(groupModel.getName())
                .members(getMembers(keycloakSession, groupModel))
                .meta(Meta.builder().created(getCreated(groupModel)).lastModified(getLastModified(groupModel)).build())
                .build();
  }

  /**
   * gets the members of the given group
   *
   * @param groupModel the group from which the members should be extracted
   * @return a list of the group members
   */
  private List<Member> getMembers(KeycloakSession keycloakSession, GroupModel groupModel)
  {
    List<Member> members = new ArrayList<>();

    keycloakSession.users()
                   .getGroupMembersStream(keycloakSession.getContext().getRealm(), groupModel)
                   .map(groupMember -> Member.builder().value(groupMember.getId()).type("User").build())
                   .forEach(members::add);

    groupModel.getSubGroupsStream()
              .map(subgroup -> Member.builder().value(subgroup.getId()).type("Group").build())
              .forEach(members::add);

    return members;
  }

  /**
   * gets the created value of the group
   *
   * @param groupModel the group model from which the created value should be extracted
   * @return the created value of the given group
   */
  private Instant getCreated(GroupModel groupModel)
  {
    String createdString = groupModel.getFirstAttribute(AttributeNames.RFC7643.CREATED);
    if (StringUtils.isNotBlank(createdString))
    {
      return Instant.ofEpochMilli(Long.parseLong(createdString));
    }
    else
    {
      return Instant.now();
    }
  }

  /**
   * gets the lastModified value of the group
   *
   * @param groupModel the group model from which the last modified value should be extracted
   * @return the last modified value of the given group
   */
  private Instant getLastModified(GroupModel groupModel)
  {
    String lastModifiedString = groupModel.getFirstAttribute(AttributeNames.RFC7643.LAST_MODIFIED);
    if (StringUtils.isNotBlank(lastModifiedString))
    {
      return Instant.ofEpochMilli(Integer.parseInt(lastModifiedString));
    }
    else
    {
      return getCreated(groupModel);
    }
  }
}
