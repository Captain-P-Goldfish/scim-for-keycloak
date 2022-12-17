package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.UserEntity;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
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
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimAddressEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimCertificatesEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimEmailsEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimEntitlementEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimImsEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimPhonesEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimPhotosEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
import de.captaingoldfish.scim.sdk.keycloak.provider.ScimJpaUserProvider;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractScimEndpointTest;
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimConfigurationBridge;
import de.captaingoldfish.scim.sdk.keycloak.scim.endpoints.CustomUser2Endpoint;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CountryUserExtension;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 10.12.2022
 */
@Slf4j
public class UserHandler2Test extends AbstractScimEndpointTest
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
                                                                       .type("work")
                                                                       .display("*******")
                                                                       .primary(true)
                                                                       .build(),
                                                            PhoneNumber.builder()
                                                                       .value(String.valueOf(random.nextLong()
                                                                                             + Integer.MAX_VALUE))
                                                                       .build()))
                                .addresses(Arrays.asList(Address.builder()
                                                                .formatted("Max Street 586")
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
                                .ims(Arrays.asList(Ims.builder()
                                                      .value("bla@bla")
                                                      .display("blubb")
                                                      .type("home")
                                                      .primary(true)
                                                      .build(),
                                                   Ims.builder().value("hepp@zep").build()))
                                .photos(Arrays.asList(Photo.builder().value("photo-1").primary(true).build(),
                                                      Photo.builder()
                                                           .value("photo-2")
                                                           .display("useless")
                                                           .type("work")
                                                           .build()))
                                .entitlements(Arrays.asList(Entitlement.builder().value("ent-1").primary(true).build(),
                                                            Entitlement.builder()
                                                                       .value("ent-2")
                                                                       .type("home")
                                                                       .display("number-2")
                                                                       .build()))
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

    ScimUserAttributesEntity userAttributes = ScimJpaUserProvider.findUserById(getKeycloakSession(), userId);
    Assertions.assertNotNull(userAttributes);
    checkUserEquality(pw, user, createdUser, userAttributes);
    checkForAdminEvent(createdUser, OperationType.CREATE);
  }

  /**
   * verifies that created users can also be deleted again
   */
  @Test
  public void testDeleteUser()
  {
    User superMarioScim = JsonHelper.loadJsonDocument(USER_SUPER_MARIO, User.class);
    User donkeyKongScim = JsonHelper.loadJsonDocument(USER_DONKEY_KONG, User.class);
    User linkScim = JsonHelper.loadJsonDocument(USER_LINK, User.class);

    superMarioScim = createUser(superMarioScim);
    donkeyKongScim = createUser(donkeyKongScim);
    linkScim = createUser(linkScim);

    deleteUser(superMarioScim);
    deleteUser(donkeyKongScim);
    deleteUser(linkScim);

    Assertions.assertEquals(0, countEntriesInTable(ScimAddressEntity.class));
    Assertions.assertEquals(0, countEntriesInTable(ScimCertificatesEntity.class));
    Assertions.assertEquals(0, countEntriesInTable(ScimEmailsEntity.class));
    Assertions.assertEquals(0, countEntriesInTable(ScimEntitlementEntity.class));
    Assertions.assertEquals(0, countEntriesInTable(ScimImsEntity.class));
    Assertions.assertEquals(0, countEntriesInTable(ScimPhonesEntity.class));
    Assertions.assertEquals(0, countEntriesInTable(ScimPhotosEntity.class));
    Assertions.assertEquals(0, countEntriesInTable(ScimUserAttributesEntity.class));
    // the admin user still remains within the database
    Assertions.assertEquals(1, countEntriesInTable(UserEntity.class));

    checkForAdminEvent(superMarioScim, OperationType.DELETE);
    checkForAdminEvent(donkeyKongScim, OperationType.DELETE);
    checkForAdminEvent(linkScim, OperationType.DELETE);
  }


  private void checkUserEquality(String pw,
                                 CustomUser user,
                                 CustomUser createdUser,
                                 ScimUserAttributesEntity userAttributes)
  {
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

    checkAddresses(user.getAddresses(), userAttributes.getAddresses());
    checkCertificates(user.getX509Certificates(), userAttributes.getCertificates());
    checkEmails(user.getEmails(), userAttributes.getEmails());
    checkEntitlements(user.getEntitlements(), userAttributes.getEntitlements());
    checkIms(user.getIms(), userAttributes.getInstantMessagingAddresses());
    checkPhoneNumbers(user.getPhoneNumbers(), userAttributes.getPhoneNumbers());
    checkPhotos(user.getPhotos(), userAttributes.getPhotos());

    UserCredentialManager credentialManager = getKeycloakSession().userCredentialManager();
    UserModel userModel = new UserAdapter(getKeycloakSession(), getRealmModel(), getEntityManager(),
                                          userAttributes.getUserEntity());
    UserCredentialModel userCredential = UserCredentialModel.password(pw);
    Assertions.assertTrue(credentialManager.isValid(getRealmModel(), userModel, userCredential));
  }

  private void checkAddresses(List<Address> expectedAddresses, List<ScimAddressEntity> actualAddresses)
  {
    Assertions.assertEquals(expectedAddresses.size(), actualAddresses.size());
    for ( int i = 0 ; i < expectedAddresses.size() ; i++ )
    {
      Address expectedAddress = expectedAddresses.get(i);
      ScimAddressEntity actualAddress = actualAddresses.get(i);
      Assertions.assertEquals(expectedAddress.getFormatted().orElse(null), actualAddress.getFormatted());
      Assertions.assertEquals(expectedAddress.getStreetAddress().orElse(null), actualAddress.getStreetAddress());
      Assertions.assertEquals(expectedAddress.getLocality().orElse(null), actualAddress.getLocality());
      Assertions.assertEquals(expectedAddress.getRegion().orElse(null), actualAddress.getRegion());
      Assertions.assertEquals(expectedAddress.getPostalCode().orElse(null), actualAddress.getPostalCode());
      Assertions.assertEquals(expectedAddress.getCountry().orElse(null), actualAddress.getCountry());
      Assertions.assertEquals(expectedAddress.getType().orElse(null), actualAddress.getType());
      Assertions.assertEquals(expectedAddress.isPrimary(), actualAddress.isPrimary());
    }
  }

  private void checkCertificates(List<ScimX509Certificate> expectedCertificates,
                                 List<ScimCertificatesEntity> actualCertificates)
  {
    Assertions.assertEquals(expectedCertificates.size(), actualCertificates.size());
    for ( int i = 0 ; i < expectedCertificates.size() ; i++ )
    {
      ScimX509Certificate expectedCertificate = expectedCertificates.get(i);
      ScimCertificatesEntity actualCertificate = actualCertificates.get(i);
      Assertions.assertEquals(expectedCertificate.getValue().orElse(null), actualCertificate.getValue());
      Assertions.assertEquals(expectedCertificate.getDisplay().orElse(null), actualCertificate.getDisplay());
      Assertions.assertEquals(expectedCertificate.getType().orElse(null), actualCertificate.getType());
      Assertions.assertEquals(expectedCertificate.isPrimary(), actualCertificate.isPrimary());
    }
  }

  private void checkEmails(List<Email> expectedEmails, List<ScimEmailsEntity> actualEmails)
  {
    Assertions.assertEquals(expectedEmails.size(), actualEmails.size());
    for ( int i = 0 ; i < expectedEmails.size() ; i++ )
    {
      Email expectedEmail = expectedEmails.get(i);
      ScimEmailsEntity actualEmail = actualEmails.get(i);
      Assertions.assertEquals(expectedEmail.getValue().orElse(null), actualEmail.getValue());
      Assertions.assertEquals(expectedEmail.getType().orElse(null), actualEmail.getType());
      Assertions.assertEquals(expectedEmail.isPrimary(), actualEmail.isPrimary());
    }
  }

  private void checkEntitlements(List<Entitlement> expectedEntitlements, List<ScimEntitlementEntity> actualEntitlements)
  {
    Assertions.assertEquals(expectedEntitlements.size(), actualEntitlements.size());
    for ( int i = 0 ; i < expectedEntitlements.size() ; i++ )
    {
      Entitlement expectedEntitlement = expectedEntitlements.get(i);
      ScimEntitlementEntity actualEntitlement = actualEntitlements.get(i);
      Assertions.assertEquals(expectedEntitlement.getValue().orElse(null), actualEntitlement.getValue());
      Assertions.assertEquals(expectedEntitlement.getDisplay().orElse(null), actualEntitlement.getDisplay());
      Assertions.assertEquals(expectedEntitlement.getType().orElse(null), actualEntitlement.getType());
      Assertions.assertEquals(expectedEntitlement.isPrimary(), actualEntitlement.isPrimary());
    }
  }

  private void checkIms(List<Ims> expectedImsList, List<ScimImsEntity> actualImsList)
  {
    Assertions.assertEquals(expectedImsList.size(), actualImsList.size());
    for ( int i = 0 ; i < expectedImsList.size() ; i++ )
    {
      Ims expectedIms = expectedImsList.get(i);
      ScimImsEntity actualIms = actualImsList.get(i);
      Assertions.assertEquals(expectedIms.getValue().orElse(null), actualIms.getValue());
      Assertions.assertEquals(expectedIms.getDisplay().orElse(null), actualIms.getDisplay());
      Assertions.assertEquals(expectedIms.getType().orElse(null), actualIms.getType());
      Assertions.assertEquals(expectedIms.isPrimary(), actualIms.isPrimary());
    }
  }

  private void checkPhoneNumbers(List<PhoneNumber> expectedPhoneNumbers, List<ScimPhonesEntity> actualPhoneNumbers)
  {
    Assertions.assertEquals(expectedPhoneNumbers.size(), actualPhoneNumbers.size());
    for ( int i = 0 ; i < expectedPhoneNumbers.size() ; i++ )
    {
      PhoneNumber expectedPhoneNumber = expectedPhoneNumbers.get(i);
      ScimPhonesEntity actualPhoneNumber = actualPhoneNumbers.get(i);
      Assertions.assertEquals(expectedPhoneNumber.getValue().orElse(null), actualPhoneNumber.getValue());
      Assertions.assertEquals(expectedPhoneNumber.getDisplay().orElse(null), actualPhoneNumber.getDisplay());
      Assertions.assertEquals(expectedPhoneNumber.getType().orElse(null), actualPhoneNumber.getType());
      Assertions.assertEquals(expectedPhoneNumber.isPrimary(), actualPhoneNumber.isPrimary());
    }
  }

  private void checkPhotos(List<Photo> expectedPhotos, List<ScimPhotosEntity> actualPhotos)
  {
    Assertions.assertEquals(expectedPhotos.size(), actualPhotos.size());
    for ( int i = 0 ; i < expectedPhotos.size() ; i++ )
    {
      Photo expectedPhoto = expectedPhotos.get(i);
      ScimPhotosEntity actualPhoto = actualPhotos.get(i);
      Assertions.assertEquals(expectedPhoto.getValue().orElse(null), actualPhoto.getValue());
      Assertions.assertEquals(expectedPhoto.getDisplay().orElse(null), actualPhoto.getDisplay());
      Assertions.assertEquals(expectedPhoto.getType().orElse(null), actualPhoto.getType());
      Assertions.assertEquals(expectedPhoto.isPrimary(), actualPhoto.isPrimary());
    }
  }

  private void checkForAdminEvent(User user, OperationType operationType)
  {
    // check for created admin event
    List<AdminEvent> adminEventList = getAdminEventStoreProvider().createAdminQuery()
                                                                  .getResultStream()
                                                                  .collect(Collectors.toList());
    AdminEvent adminEvent = adminEventList.stream()
                                          .filter(event -> event.getOperationType().equals(operationType)
                                                           && event.getResourcePath()
                                                                   .equals("users/" + user.getId().get()))
                                          .findAny()
                                          .orElse(null);
    Assertions.assertNotNull(adminEvent);
    Assertions.assertEquals(getTestClient().getId(), adminEvent.getAuthDetails().getClientId());
    Assertions.assertEquals(getTestUser().getId(), adminEvent.getAuthDetails().getUserId());
    Assertions.assertEquals("users/" + user.getId().get(), adminEvent.getResourcePath());
    Assertions.assertEquals(operationType, adminEvent.getOperationType());
    Assertions.assertEquals(org.keycloak.events.admin.ResourceType.USER, adminEvent.getResourceType());
    // equalize the two objects by modifying the meta-attribute. The meta-attribute is not identical because the
    // schema-validation is modifying the meta-attribute when evaluating the response
    User adminEventUser = JsonHelper.readJsonDocument(adminEvent.getRepresentation(), User.class);
    // the structure of the user is not so important here but that it is a JSON SCIM user that was persisted
    Assertions.assertTrue(adminEventUser.getSchemas().contains(SchemaUris.USER_URI));
  }
}
