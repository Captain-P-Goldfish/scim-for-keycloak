package de.captaingoldfish.scim.sdk.keycloak.setup;

import java.util.Collections;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Assertions;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.credential.PasswordCredentialProviderFactory;
import org.keycloak.credential.UserCredentialStoreManager;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProviderFactory;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.policy.DefaultPasswordPolicyManagerProvider;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.services.DefaultKeycloakContext;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.DefaultKeycloakTransactionManager;
import org.mockito.Mockito;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 05.08.2020
 */
@Slf4j
class KeycloakMockSetup
{

  private static final String TEST_REALM_NAME = "SCIM";

  /**
   * a keycloak session mock
   */
  private KeycloakSession keycloakSession;

  /**
   * used to setup some default database settings
   */
  private EntityManager entityManager;

  /**
   * a context that is placed within the keycloakSession
   */
  private KeycloakContext keycloakContext;

  /**
   * the custom realm for our unit tests
   */
  @Getter
  private RealmModel realmModel;

  /**
   * the user that is used for unit testing
   */
  @Getter
  private UserModel user;

  /**
   * the keycloak session factory
   */
  @Getter
  private KeycloakSessionFactory keycloakSessionFactory;

  /**
   * the transaction manager that is used by keycloak
   */
  @Getter
  private DefaultKeycloakTransactionManager keycloakTransactionManager;

  public KeycloakMockSetup(KeycloakSession keycloakSession, EntityManager entityManager)
  {
    this.keycloakSession = keycloakSession;
    this.entityManager = entityManager;
    this.keycloakContext = Mockito.spy(new DefaultKeycloakContext(keycloakSession));
    Mockito.doReturn(keycloakContext).when(this.keycloakSession).getContext();
    this.keycloakSessionFactory = Mockito.spy(new DefaultKeycloakSessionFactory());
    Mockito.doReturn(keycloakSessionFactory).when(this.keycloakSession).getKeycloakSessionFactory();
    keycloakTransactionManager = Mockito.spy(new DefaultKeycloakTransactionManager(keycloakSession));
    Mockito.doReturn(keycloakTransactionManager).when(keycloakSession).getTransactionManager();
    Mockito.doReturn(keycloakSession).when(keycloakSessionFactory).create();
    setupPasswordManagingSettings();
  }

  /**
   * setups the password managing configuration for testing
   */
  protected void setupPasswordManagingSettings()
  {
    Mockito.doReturn(new UserCredentialStoreManager(keycloakSession)).when(keycloakSession).userCredentialManager();
    Mockito.doReturn(Collections.singletonList(new PasswordCredentialProviderFactory()))
           .when(keycloakSessionFactory)
           .getProviderFactories(CredentialProvider.class);
    Mockito.doReturn(new PasswordCredentialProvider(keycloakSession))
           .when(keycloakSession)
           .getProvider(CredentialProvider.class, PasswordCredentialProviderFactory.PROVIDER_ID);
    Mockito.doReturn(new DefaultPasswordPolicyManagerProvider(keycloakSession))
           .when(keycloakSession)
           .getProvider(PasswordPolicyManagerProvider.class);
    PasswordHashProvider passwordHashProvider = new Pbkdf2PasswordHashProviderFactory().create(keycloakSession);
    Mockito.doReturn(passwordHashProvider)
           .when(keycloakSession)
           .getProvider(PasswordHashProvider.class, "pbkdf2-sha256");
    Mockito.doReturn(passwordHashProvider).when(keycloakSession).getProvider(PasswordHashProvider.class, "pbkdf2");
    Mockito.doReturn(new UserCredentialStoreManager(keycloakSession)).when(keycloakSession).userCredentialManager();
  }

  /**
   * will create the realm that we are going to use
   */
  public final void createRealm()
  {
    log.trace("building test realm '{}'", TEST_REALM_NAME);
    entityManager.getTransaction().begin();
    realmModel = keycloakSession.realms().createRealm(UUID.randomUUID().toString(), TEST_REALM_NAME);
    realmModel.setPasswordPolicy(PasswordPolicy.build().build(keycloakSession));
    entityManager.getTransaction().commit();
    Mockito.doReturn(realmModel).when(keycloakContext).getRealm();
    Assertions.assertEquals(1, keycloakSession.realms().getRealms().size());
    log.debug("test-realm successfully created: {} - {}", realmModel.getId(), realmModel.getName());
    createUser();
  }

  /**
   * creates a user for the current realm
   */
  private void createUser()
  {
    entityManager.getTransaction().begin();
    user = keycloakSession.users().addUser(realmModel, "admin");
    user.setSingleAttribute("scim-user", String.valueOf(true));
    entityManager.getTransaction().commit();
  }


}
