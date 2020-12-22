package de.captaingoldfish.scim.sdk.keycloak.services;

import javax.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

import lombok.AccessLevel;
import lombok.Getter;


/**
 * @author Pascal Knueppel
 * @since 02.08.2020
 */
public class AbstractService
{

  /**
   * the current request context
   */
  @Getter(AccessLevel.PROTECTED)
  private final KeycloakSession keycloakSession;

  public AbstractService(KeycloakSession keycloakSession)
  {
    this.keycloakSession = keycloakSession;
  }

  protected EntityManager getEntityManager()
  {
    return getKeycloakSession().getProvider(JpaConnectionProvider.class).getEntityManager();
  }
}
