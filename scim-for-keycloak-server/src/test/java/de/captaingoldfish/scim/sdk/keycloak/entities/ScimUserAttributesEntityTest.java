package de.captaingoldfish.scim.sdk.keycloak.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;

import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;


/**
 * @author Pascal Knueppel
 * @since 09.12.2022
 */
public class ScimUserAttributesEntityTest extends KeycloakScimManagementTest
{

  @Test
  public void testCreateUserWithScimAttributes()
  {
    UserModel userModel = getKeycloakSession().users().addUser(getRealmModel(), "max");

    List<ScimEmailsEntity> emails = new ArrayList<>();
    emails.add(ScimEmailsEntity.builder().value("max.mustermann@gmx.de").type("home").primary(true).build());
    emails.add(ScimEmailsEntity.builder().value("max.mustermann@work.de").type("work").build());

    ScimUserAttributesEntity userAttributes = ScimUserAttributesEntity.builder()
                                                                      .userEntity(((UserAdapter)userModel).getEntity())
                                                                      .externalId(UUID.randomUUID().toString())
                                                                      .nameFormatted("Max Mustermann")
                                                                      .givenName("max")
                                                                      .middleName("muster")
                                                                      .familyName("mustermann")
                                                                      .nameHonorificPrefix("Mr.")
                                                                      .nameHonorificSuffix("M.D.")
                                                                      .displayName("Max Mustermann")
                                                                      .nickName("maxi")
                                                                      .profileUrl("http://localhost/maxi")
                                                                      .title("A.D.")
                                                                      .userType("admin")
                                                                      .preferredLanguage("de")
                                                                      .locale("de")
                                                                      .timezone("Europe/Berlin")
                                                                      .emails(emails)
                                                                      .build();
    getEntityManager().persist(userAttributes);
    getEntityManager().flush();

    ScimUserAttributesEntity dbUserAttributes = getEntityManager().find(ScimUserAttributesEntity.class,
                                                                        userAttributes.getId());
    Assertions.assertEquals(2, dbUserAttributes.getEmails().size());
  }
}
