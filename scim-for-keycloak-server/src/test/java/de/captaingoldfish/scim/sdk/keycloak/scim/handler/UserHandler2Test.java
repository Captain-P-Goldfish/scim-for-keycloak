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
import de.captaingoldfish.scim.sdk.keycloak.scim.ScimConfiguration;
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

  @Test
  public void testUpdateUser()
  {
    ServiceProvider serviceProvider = ScimConfiguration.getScimEndpoint(getKeycloakSession(), false)
                                                       .getServiceProvider();
    serviceProvider.setChangePasswordConfig(ChangePasswordConfig.builder().supported(true).build());

    CustomUser superMarioScim = JsonHelper.loadJsonDocument(USER_SUPER_MARIO, CustomUser.class);
    CustomUser linkScim = JsonHelper.loadJsonDocument(USER_LINK, CustomUser.class);

    superMarioScim = createUser(superMarioScim);
    final String userId = superMarioScim.getId().get();

    // now update the data of mario with links data
    CustomUser updatedUser = updateUser(userId, linkScim);

    ScimUserAttributesEntity userAttributes = ScimJpaUserProvider.findUserById(getKeycloakSession(), userId);
    String password = linkScim.getPassword().get();
    checkUserEquality(password, linkScim, updatedUser, userAttributes);
  }

  private void checkUserEquality(String pw,
                                 CustomUser user,
                                 CustomUser createdUser,
                                 ScimUserAttributesEntity userAttributes)
  {
    Assertions.assertEquals(user.getUserName().orElse(null), createdUser.getUserName().orElse(null));
    Assertions.assertEquals(user.getUserName().orElse(null), userAttributes.getUserEntity().getUsername());

    Assertions.assertEquals(user.getExternalId().orElse(null), createdUser.getExternalId().orElse(null));
    Assertions.assertEquals(user.getExternalId().orElse(null), userAttributes.getExternalId());

    Assertions.assertEquals(user.getName().orElse(null).getFormatted().orElse(null),
                            createdUser.getName().orElse(null).getFormatted().orElse(null));
    Assertions.assertEquals(user.getName().orElse(null).getFormatted().orElse(null), userAttributes.getNameFormatted());

    Assertions.assertEquals(user.getName().orElse(null).getGivenName().orElse(null),
                            createdUser.getName().orElse(null).getGivenName().orElse(null));
    Assertions.assertEquals(user.getName().orElse(null).getGivenName().orElse(null), userAttributes.getGivenName());

    Assertions.assertEquals(user.getName().orElse(null).getFamilyName().orElse(null),
                            createdUser.getName().orElse(null).getFamilyName().orElse(null));
    Assertions.assertEquals(user.getName().orElse(null).getFamilyName().orElse(null), userAttributes.getFamilyName());

    Assertions.assertEquals(user.getName().orElse(null).getMiddleName().orElse(null),
                            createdUser.getName().orElse(null).getMiddleName().orElse(null));
    Assertions.assertEquals(user.getName().orElse(null).getMiddleName().orElse(null), userAttributes.getMiddleName());

    Assertions.assertEquals(user.getName().orElse(null).getHonorificPrefix().orElse(null),
                            createdUser.getName().orElse(null).getHonorificPrefix().orElse(null));
    Assertions.assertEquals(user.getName().orElse(null).getHonorificPrefix().orElse(null),
                            userAttributes.getNameHonorificPrefix());

    Assertions.assertEquals(user.getName().orElse(null).getHonorificSuffix().orElse(null),
                            createdUser.getName().orElse(null).getHonorificSuffix().orElse(null));
    Assertions.assertEquals(user.getName().orElse(null).getHonorificSuffix().orElse(null),
                            userAttributes.getNameHonorificSuffix());

    Assertions.assertEquals(user.isActive().orElse(null), createdUser.isActive().orElse(null));
    Assertions.assertEquals(user.isActive().orElse(null), userAttributes.getUserEntity().isEnabled());

    Assertions.assertEquals(user.getNickName().orElse(null), createdUser.getNickName().orElse(null));
    Assertions.assertEquals(user.getNickName().orElse(null), userAttributes.getNickName());

    Assertions.assertEquals(user.getTitle().orElse(null), createdUser.getTitle().orElse(null));
    Assertions.assertEquals(user.getTitle().orElse(null), userAttributes.getTitle());

    Assertions.assertEquals(user.getDisplayName().orElse(null), createdUser.getDisplayName().orElse(null));
    Assertions.assertEquals(user.getDisplayName().orElse(null), userAttributes.getDisplayName());

    Assertions.assertEquals(user.getUserType().orElse(null), createdUser.getUserType().orElse(null));
    Assertions.assertEquals(user.getUserType().orElse(null), userAttributes.getUserType());

    Assertions.assertEquals(user.getLocale().orElse(null), createdUser.getLocale().orElse(null));
    Assertions.assertEquals(user.getLocale().orElse(null), userAttributes.getLocale());

    Assertions.assertEquals(user.getPreferredLanguage().orElse(null), createdUser.getPreferredLanguage().orElse(null));
    Assertions.assertEquals(user.getPreferredLanguage().orElse(null), userAttributes.getPreferredLanguage());

    Assertions.assertEquals(user.getTimezone().orElse(null), createdUser.getTimezone().orElse(null));
    Assertions.assertEquals(user.getTimezone().orElse(null), userAttributes.getTimezone());

    Assertions.assertEquals(user.getProfileUrl().orElse(null), createdUser.getProfileUrl().orElse(null));
    Assertions.assertEquals(user.getProfileUrl().orElse(null), userAttributes.getProfileUrl());

    Assertions.assertEquals(user.getProfileUrl().orElse(null), createdUser.getProfileUrl().orElse(null));
    Assertions.assertEquals(user.getProfileUrl().orElse(null), userAttributes.getProfileUrl());

    Assertions.assertEquals(user.getEnterpriseUser().flatMap(EnterpriseUser::getEmployeeNumber).orElse(null),
                            createdUser.getEnterpriseUser().flatMap(EnterpriseUser::getEmployeeNumber).orElse(null));
    Assertions.assertEquals(user.getEnterpriseUser().flatMap(EnterpriseUser::getEmployeeNumber).orElse(null),
                            userAttributes.getEmployeeNumber());

    Assertions.assertEquals(user.getEnterpriseUser().flatMap(EnterpriseUser::getDepartment).orElse(null),
                            createdUser.getEnterpriseUser().flatMap(EnterpriseUser::getDepartment).orElse(null));
    Assertions.assertEquals(user.getEnterpriseUser().flatMap(EnterpriseUser::getDepartment).orElse(null),
                            userAttributes.getDepartment());

    Assertions.assertEquals(user.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).orElse(null),
                            createdUser.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).orElse(null));
    Assertions.assertEquals(user.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).orElse(null),
                            userAttributes.getCostCenter());

    Assertions.assertEquals(user.getEnterpriseUser().flatMap(EnterpriseUser::getDivision).orElse(null),
                            createdUser.getEnterpriseUser().flatMap(EnterpriseUser::getDivision).orElse(null));
    Assertions.assertEquals(user.getEnterpriseUser().flatMap(EnterpriseUser::getDivision).orElse(null),
                            userAttributes.getDivision());

    Assertions.assertEquals(user.getEnterpriseUser().flatMap(EnterpriseUser::getOrganization).orElse(null),
                            createdUser.getEnterpriseUser().flatMap(EnterpriseUser::getOrganization).orElse(null));
    Assertions.assertEquals(user.getEnterpriseUser().flatMap(EnterpriseUser::getOrganization).orElse(null),
                            userAttributes.getOrganization());

    Assertions.assertEquals(user.getEnterpriseUser()
                                .flatMap(EnterpriseUser::getManager)
                                .flatMap(Manager::getValue)
                                .orElse(null),
                            createdUser.getEnterpriseUser()
                                       .flatMap(EnterpriseUser::getManager)
                                       .flatMap(Manager::getValue)
                                       .orElse(null));
    Assertions.assertEquals(user.getEnterpriseUser()
                                .flatMap(EnterpriseUser::getManager)
                                .flatMap(Manager::getValue)
                                .orElse(null),
                            userAttributes.getManagerValue());

    Assertions.assertEquals(user.getEnterpriseUser()
                                .flatMap(EnterpriseUser::getManager)
                                .flatMap(Manager::getRef)
                                .orElse(null),
                            createdUser.getEnterpriseUser()
                                       .flatMap(EnterpriseUser::getManager)
                                       .flatMap(Manager::getRef)
                                       .orElse(null));
    Assertions.assertEquals(user.getEnterpriseUser().orElse(null).getManager().flatMap(Manager::getRef).orElse(null),
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
    Assertions.assertTrue(credentialManager.isValid(getRealmModel(), userModel, userCredential),
                          "Password verification has failed");
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
