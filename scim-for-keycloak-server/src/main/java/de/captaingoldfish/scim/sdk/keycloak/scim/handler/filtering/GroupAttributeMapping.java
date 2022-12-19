package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering;

import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.GROUPS_ENTITY;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.USER_ENTITY;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.USER_GROUPS_MEMBERSHIP;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames.RFC7643;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.AbstractAttributeMapping;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpqlTableJoin;


/**
 * @author Pascal Knueppel
 * @since 19.12.2022
 */
public class GroupAttributeMapping extends AbstractAttributeMapping
{

  private static final GroupAttributeMapping GROUP_ATTRIBUTE_MAPPING = new GroupAttributeMapping();

  private GroupAttributeMapping()
  {
    /*
     * @formatter:off
     *
     * these joins will all together represent a sql like this:
     *
     * select distinct g from GroupEntity g
     *   left join UserGroupMembershipEntity ugm on g.id = ugm.groupId
     *   left join UserEntity u on ugm.user.id = u.id
     *
     * @formatter:on
     */
    JpqlTableJoin baseJpqlTableJoin = new JpqlTableJoin(GROUPS_ENTITY);
    JpqlTableJoin userMembershipJoin = new JpqlTableJoin(GROUPS_ENTITY, USER_GROUPS_MEMBERSHIP, "id", "groupId", false);
    JpqlTableJoin usersJoin = new JpqlTableJoin(USER_GROUPS_MEMBERSHIP, USER_ENTITY, "user.id", "id", false);

    addAttribute(SchemaUris.GROUP_URI, null, RFC7643.ID, "id", baseJpqlTableJoin);
    addAttribute(SchemaUris.GROUP_URI, null, RFC7643.DISPLAY_NAME, "name", baseJpqlTableJoin);

    addAttribute(SchemaUris.GROUP_URI, RFC7643.MEMBERS, RFC7643.VALUE, "id", userMembershipJoin, usersJoin);
    addAttribute(SchemaUris.GROUP_URI, RFC7643.MEMBERS, RFC7643.DISPLAY, "username", userMembershipJoin, usersJoin);
  }

  public static GroupAttributeMapping getInstance()
  {
    return GROUP_ATTRIBUTE_MAPPING;
  }
}
