package de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup;

import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.GROUPS_ENTITY;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.SCIM_EMAILS;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.SCIM_USER_ATTRIBUTES;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.USER_ENTITY;
import static de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.filtersetup.JpaEntityReferences.USER_GROUPS_MEMBERSHIP;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractScimEndpointTest;
import de.captaingoldfish.scim.sdk.keycloak.scim.handler.filtering.UserAttributeMapping;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 16.12.2022
 */
@Slf4j
public class AbstractFilteringTest extends AbstractScimEndpointTest
{

  @Test
  public void testSortJoins()
  {
    JpqlTableJoin userAttributesJoin = new JpqlTableJoin(USER_ENTITY, SCIM_USER_ATTRIBUTES, "id", "userEntity.id",
                                                         true);
    JpqlTableJoin emailsJoin = new JpqlTableJoin(SCIM_USER_ATTRIBUTES, SCIM_EMAILS, "id", "userAttributes.id", false);
    JpqlTableJoin groupMembershipJoin = new JpqlTableJoin(USER_ENTITY, USER_GROUPS_MEMBERSHIP, "id", "user.id", false);
    JpqlTableJoin groupsJoin = new JpqlTableJoin(USER_GROUPS_MEMBERSHIP, GROUPS_ENTITY, "groupId", "id", false);

    TestUserFiltering testUserFiltering = new TestUserFiltering();
    testUserFiltering.getJoins().addAll(Arrays.asList(userAttributesJoin, emailsJoin, groupMembershipJoin, groupsJoin));

    List<JpqlTableJoin> sortedJoins = testUserFiltering.getSortedJoins();

    Assertions.assertEquals(testUserFiltering.getJoins().size(), sortedJoins.size());
    log.warn("\n{}", sortedJoins.stream().map(JpqlTableJoin::toString).collect(Collectors.joining(",\n")));
    MatcherAssert.assertThat(sortedJoins,
                             Matchers.contains(Matchers.equalTo(userAttributesJoin),
                                               Matchers.equalTo(groupMembershipJoin),
                                               Matchers.equalTo(groupsJoin),
                                               Matchers.equalTo(emailsJoin)));
  }

  @Test
  public void testSortJoins2()
  {
    JpqlTableJoin groupMembershipJoin = new JpqlTableJoin(USER_ENTITY, USER_GROUPS_MEMBERSHIP, "id", "user.id", false);
    JpqlTableJoin groupsJoin = new JpqlTableJoin(USER_GROUPS_MEMBERSHIP, GROUPS_ENTITY, "groupId", "id", false);

    TestUserFiltering testUserFiltering = new TestUserFiltering();
    testUserFiltering.getJoins().addAll(Arrays.asList(groupMembershipJoin, groupsJoin));

    List<JpqlTableJoin> sortedJoins = testUserFiltering.getSortedJoins();

    Assertions.assertEquals(testUserFiltering.getJoins().size(), sortedJoins.size());
    log.warn("\n{}", sortedJoins.stream().map(JpqlTableJoin::toString).collect(Collectors.joining(",\n")));
    MatcherAssert.assertThat(sortedJoins,
                             Matchers.contains(Matchers.equalTo(groupMembershipJoin), Matchers.equalTo(groupsJoin)));
  }

  private class TestUserFiltering extends AbstractFiltering<ScimUserAttributesEntity>
  {

    public TestUserFiltering()
    {
      super(getKeycloakSession(), new UserAttributeMapping(), 1, 0, null, null, null);
    }

    @Override
    protected JpaEntityReferences getBaseEntity()
    {
      return JpaEntityReferences.USER_ENTITY;
    }

    @Override
    protected String getRealmRestrictionClause()
    {
      return "";
    }

    @Override
    protected List<ScimUserAttributesEntity> parseResultStream(Stream<Object[]> resultStream)
    {
      return null;
    }
  }
}
