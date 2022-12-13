package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.ChangePasswordConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Address;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Entitlement;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Ims;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PhoneNumber;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Photo;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.ScimX509Certificate;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimEmailsEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractScimEndpointTest;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimConfigurationBridge;
import de.captaingoldfish.scim.sdk.keycloak.scim.endpoints.CustomUser2Endpoint;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CountryUserExtension;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;
import de.captaingoldfish.scim.sdk.keycloak.setup.FileReferences;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 10.12.2022
 */
@Slf4j
public class UserHandler2Test extends AbstractScimEndpointTest implements FileReferences
{

  @Test
  public void testCreateUser()
  {
    ResourceEndpoint resourceEndpoint = ScimConfigurationBridge.getScimResourceEndpoints()
                                                               .get(getRealmModel().getName());
    ServiceProvider serviceProvider = resourceEndpoint.getServiceProvider();
    serviceProvider.setChangePasswordConfig(ChangePasswordConfig.builder().supported(true).build());

    Random random = new Random();
    String name = "goldfish";
    String pw = UUID.randomUUID().toString();

    CountryUserExtension countryUserExtension = CountryUserExtension.builder()
                                                                    .countries(Arrays.asList("germany", "italy"))
                                                                    .businessLine(Arrays.asList("1", "2"))
                                                                    .build();
    CustomUser user = CustomUser.builder()
                                .countryUserExtension(countryUserExtension)
                                .userName(name)
                                .externalId(UUID.randomUUID().toString())
                                .name(Name.builder()
                                          .givenName(name + "_")
                                          .middlename(UUID.randomUUID().toString())
                                          .familyName("Mustermann")
                                          .honorificPrefix("Mr.")
                                          .honorificSuffix("sama")
                                          .formatted(name + "____")
                                          .build())
                                .active(random.nextBoolean())
                                .nickName(name + "+++")
                                .title("Dr.")
                                .displayName(name + "****")
                                .userType("admin")
                                .locale("de-DE")
                                .preferredLanguage("de")
                                .timeZone("Europe/Berlin")
                                .profileUrl("http://localhost/" + name)
                                .password(pw)
                                .emails(Arrays.asList(Email.builder()
                                                           .value(name + "@test.de")
                                                           .primary(true)
                                                           .type("work")
                                                           .build(),
                                                      Email.builder()
                                                           .value(name + "_the_second@test.de")
                                                           .type("home")
                                                           .build()))
                                .phoneNumbers(Arrays.asList(PhoneNumber.builder()
                                                                       .value(String.valueOf(random.nextLong()
                                                                                             + Integer.MAX_VALUE))
                                                                       .primary(true)
                                                                       .build(),
                                                            PhoneNumber.builder()
                                                                       .value(String.valueOf(random.nextLong()
                                                                                             + Integer.MAX_VALUE))
                                                                       .build()))
                                .addresses(Arrays.asList(Address.builder()
                                                                .streetAddress(name + " street " + random.nextInt(500))
                                                                .country(random.nextBoolean() ? "germany"
                                                                  : "united states")
                                                                .postalCode(String.valueOf(random.nextLong()
                                                                                           + Integer.MAX_VALUE))
                                                                .primary(random.nextInt(20) == 0)
                                                                .build(),
                                                         Address.builder()
                                                                .streetAddress(name + " second street "
                                                                               + random.nextInt(500))
                                                                .country(random.nextBoolean() ? "germany"
                                                                  : "united states")
                                                                .postalCode(String.valueOf(random.nextLong()
                                                                                           + Integer.MAX_VALUE))
                                                                .build()))
                                .ims(Arrays.asList(Ims.builder().value("bla@bla").primary(true).build(),
                                                   Ims.builder().value("hepp@zep").build()))
                                .photos(Arrays.asList(Photo.builder().value("photo-1").primary(true).build(),
                                                      Photo.builder().value("photo-2").build()))
                                .entitlements(Arrays.asList(Entitlement.builder().value("ent-1").primary(true).build(),
                                                            Entitlement.builder().value("ent-2").build()))
                                .x509Certificates(Arrays.asList(ScimX509Certificate.builder()
                                                                                   .value("MII...1")
                                                                                   .primary(true)
                                                                                   .build(),
                                                                ScimX509Certificate.builder().value("MII...4").build()))
                                .enterpriseUser(EnterpriseUser.builder()
                                                              .employeeNumber(UUID.randomUUID().toString())
                                                              .department(UUID.randomUUID().toString())
                                                              .costCenter(UUID.randomUUID().toString())
                                                              .division(UUID.randomUUID().toString())
                                                              .organization(UUID.randomUUID().toString())
                                                              .manager(Manager.builder()
                                                                              .value(UUID.randomUUID().toString())
                                                                              .ref(UUID.randomUUID().toString())
                                                                              .build())
                                                              .build())
                                .build();

    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .method(HttpMethod.POST)
                                               .endpoint(CustomUser2Endpoint.CUSTOM_USER_2_ENDPOINT)
                                               .requestBody(user.toString())
                                               .build();
    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());


    CustomUser createdUser = JsonHelper.readJsonDocument((String)response.getEntity(), CustomUser.class);
    final String userId = createdUser.getId().get();

    final String queryName = ScimUserAttributesEntity.GET_SCIM_USER_ATTRIBUTES_QUERY_NAME;
    ScimUserAttributesEntity userAttributes = getEntityManager().createNamedQuery(queryName,
                                                                                  ScimUserAttributesEntity.class)
                                                                .setParameter("userId", userId)
                                                                .getSingleResult();
    Assertions.assertNotNull(userAttributes);
    Assertions.assertEquals(user.getUserName().get(), createdUser.getUserName().get());
    Assertions.assertEquals(user.getUserName().get(), userAttributes.getUserEntity().getUsername());

    Assertions.assertEquals(user.getExternalId().get(), createdUser.getExternalId().get());
    Assertions.assertEquals(user.getExternalId().get(), userAttributes.getExternalId());

    Assertions.assertEquals(user.getName().get().getFormatted().get(),
                            createdUser.getName().get().getFormatted().get());
    Assertions.assertEquals(user.getName().get().getFormatted().get(), userAttributes.getNameFormatted());

    Assertions.assertEquals(user.getName().get().getGivenName().get(),
                            createdUser.getName().get().getGivenName().get());
    Assertions.assertEquals(user.getName().get().getGivenName().get(), userAttributes.getGivenName());

    Assertions.assertEquals(user.getName().get().getFamilyName().get(),
                            createdUser.getName().get().getFamilyName().get());
    Assertions.assertEquals(user.getName().get().getFamilyName().get(), userAttributes.getFamilyName());

    Assertions.assertEquals(user.getName().get().getMiddleName().get(),
                            createdUser.getName().get().getMiddleName().get());
    Assertions.assertEquals(user.getName().get().getMiddleName().get(), userAttributes.getMiddleName());

    Assertions.assertEquals(user.getName().get().getHonorificPrefix().get(),
                            createdUser.getName().get().getHonorificPrefix().get());
    Assertions.assertEquals(user.getName().get().getHonorificPrefix().get(), userAttributes.getNameHonorificPrefix());

    Assertions.assertEquals(user.getName().get().getHonorificSuffix().get(),
                            createdUser.getName().get().getHonorificSuffix().get());
    Assertions.assertEquals(user.getName().get().getHonorificSuffix().get(), userAttributes.getNameHonorificSuffix());

    Assertions.assertEquals(user.isActive().get(), createdUser.isActive().get());
    Assertions.assertEquals(user.isActive().get(), userAttributes.getUserEntity().isEnabled());

    Assertions.assertEquals(user.getNickName().get(), createdUser.getNickName().get());
    Assertions.assertEquals(user.getNickName().get(), userAttributes.getNickName());

    Assertions.assertEquals(user.getTitle().get(), createdUser.getTitle().get());
    Assertions.assertEquals(user.getTitle().get(), userAttributes.getTitle());

    Assertions.assertEquals(user.getDisplayName().get(), createdUser.getDisplayName().get());
    Assertions.assertEquals(user.getDisplayName().get(), userAttributes.getDisplayName());

    Assertions.assertEquals(user.getUserType().get(), createdUser.getUserType().get());
    Assertions.assertEquals(user.getUserType().get(), userAttributes.getUserType());

    Assertions.assertEquals(user.getLocale().get(), createdUser.getLocale().get());
    Assertions.assertEquals(user.getLocale().get(), userAttributes.getLocale());

    Assertions.assertEquals(user.getPreferredLanguage().get(), createdUser.getPreferredLanguage().get());
    Assertions.assertEquals(user.getPreferredLanguage().get(), userAttributes.getPreferredLanguage());

    Assertions.assertEquals(user.getTimezone().get(), createdUser.getTimezone().get());
    Assertions.assertEquals(user.getTimezone().get(), userAttributes.getTimezone());

    Assertions.assertEquals(user.getProfileUrl().get(), createdUser.getProfileUrl().get());
    Assertions.assertEquals(user.getProfileUrl().get(), userAttributes.getProfileUrl());

    Assertions.assertEquals(user.getProfileUrl().get(), createdUser.getProfileUrl().get());
    Assertions.assertEquals(user.getProfileUrl().get(), userAttributes.getProfileUrl());

    Assertions.assertEquals(user.getEnterpriseUser().get().getEmployeeNumber().get(),
                            createdUser.getEnterpriseUser().get().getEmployeeNumber().get());
    Assertions.assertEquals(user.getEnterpriseUser().get().getEmployeeNumber().get(),
                            userAttributes.getEmployeeNumber());

    Assertions.assertEquals(user.getEnterpriseUser().get().getDepartment().get(),
                            createdUser.getEnterpriseUser().get().getDepartment().get());
    Assertions.assertEquals(user.getEnterpriseUser().get().getDepartment().get(), userAttributes.getDepartment());

    Assertions.assertEquals(user.getEnterpriseUser().get().getCostCenter().get(),
                            createdUser.getEnterpriseUser().get().getCostCenter().get());
    Assertions.assertEquals(user.getEnterpriseUser().get().getCostCenter().get(), userAttributes.getCostCenter());

    Assertions.assertEquals(user.getEnterpriseUser().get().getDivision().get(),
                            createdUser.getEnterpriseUser().get().getDivision().get());
    Assertions.assertEquals(user.getEnterpriseUser().get().getDivision().get(), userAttributes.getDivision());

    Assertions.assertEquals(user.getEnterpriseUser().get().getOrganization().get(),
                            createdUser.getEnterpriseUser().get().getOrganization().get());
    Assertions.assertEquals(user.getEnterpriseUser().get().getOrganization().get(), userAttributes.getOrganization());

    Assertions.assertEquals(user.getEnterpriseUser().get().getManager().get().getValue().get(),
                            createdUser.getEnterpriseUser().get().getManager().get().getValue().get());
    Assertions.assertEquals(user.getEnterpriseUser().get().getManager().get().getValue().get(),
                            userAttributes.getManagerValue());

    Assertions.assertEquals(user.getEnterpriseUser().get().getManager().get().getRef().get(),
                            createdUser.getEnterpriseUser().get().getManager().get().getRef().get());
    Assertions.assertEquals(user.getEnterpriseUser().get().getManager().get().getRef().get(),
                            userAttributes.getManagerReference());

    checkEmails(user.getEmails(), userAttributes.getEmails());

    UserCredentialManager credentialManager = getKeycloakSession().userCredentialManager();
    UserModel userModel = new UserAdapter(getKeycloakSession(), getRealmModel(), getEntityManager(),
                                          userAttributes.getUserEntity());
    UserCredentialModel userCredential = UserCredentialModel.password(pw);
    Assertions.assertTrue(credentialManager.isValid(getRealmModel(), userModel, userCredential));
  }

  private void checkEmails(List<Email> expectedEmails, List<ScimEmailsEntity> actualEmails)
  {
    Assertions.assertEquals(expectedEmails.size(), actualEmails.size());
    for ( int i = 0 ; i < expectedEmails.size() ; i++ )
    {
      Email expectedEmail = expectedEmails.get(i);
      ScimEmailsEntity actualEmail = actualEmails.get(i);
      Assertions.assertEquals(expectedEmail.getValue().orElse(null), actualEmail.getValue());
      Assertions.assertEquals(expectedEmail.getDisplay().orElse(null), actualEmail.getDisplay());
      Assertions.assertEquals(expectedEmail.getType().orElse(null), actualEmail.getType());
      Assertions.assertEquals(expectedEmail.isPrimary(), actualEmail.isPrimary());
    }
  }
}
