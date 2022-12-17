package de.captaingoldfish.scim.sdk.keycloak.provider;

import javax.persistence.EntityManager;

import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserProviderFactory;


/**
 * this factory overrides the default keycloak {@link UserProviderFactory} in order to make sure that keycloak
 * will use our custom {@link ScimJpaUserProvider} instead of the keycloak default
 * {@link org.keycloak.models.jpa.JpaUserProvider}
 * 
 * @author Pascal Knueppel
 * @since 17.12.2022
 */
public class ScimUserProviderFactory implements UserProviderFactory
{

  @Override
  public UserProvider create(KeycloakSession keycloakSession)
  {
    EntityManager em = keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();
    return new ScimJpaUserProvider(keycloakSession, em);
  }

  @Override
  public void init(Config.Scope scope)
  {

  }

  @Override
  public void postInit(KeycloakSessionFactory keycloakSessionFactory)
  {

  }

  @Override
  public void close()
  {

  }

  /**
   * this id is responsible for overriding the default {@link UserProviderFactory}
   */
  @Override
  public String getId()
  {
    return org.keycloak.models.jpa.JpaRealmProviderFactory.PROVIDER_ID;
  }
}
