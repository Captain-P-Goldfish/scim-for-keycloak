package de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup;

import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.keycloak.connections.jpa.DefaultJpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
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
 * setup a keycloak environment to create some default values within the current database and to easily access
 * the current database entries of the keycloak
 * 
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
@Slf4j
class KeycloakMockSetup
{

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
    entityManager.getTransaction().begin();
    this.realmModel = keycloakSession.realms().getRealmByName("master");
    realmModel.setPasswordPolicy(PasswordPolicy.build().build(keycloakSession));
    Mockito.doReturn(realmModel).when(keycloakContext).getRealm();
    entityManager.getTransaction().commit();
    setupPasswordManagingSettings();
    JpaConnectionProvider jpaConnectionProvider = new DefaultJpaConnectionProvider(entityManager);
    Mockito.doReturn(jpaConnectionProvider).when(keycloakSession).getProvider(JpaConnectionProvider.class);
  }

  /**
   * setups the password managing configuration for testing
   */
  protected void setupPasswordManagingSettings()
  {
    Mockito.doReturn(new UserCredentialStoreManager(keycloakSession)).when(keycloakSession).userCredentialManager();
    Mockito.doReturn(Stream.of(new PasswordCredentialProviderFactory()))
           .when(keycloakSessionFactory)
           .getProviderFactoriesStream(CredentialProvider.class);
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
}
