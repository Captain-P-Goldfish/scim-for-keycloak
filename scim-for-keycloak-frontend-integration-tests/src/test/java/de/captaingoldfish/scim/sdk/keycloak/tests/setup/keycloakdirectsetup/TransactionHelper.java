package de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup;

import javax.persistence.EntityManager;

import lombok.SneakyThrows;


/**
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
public interface TransactionHelper
{

  public EntityManager getEntityManager();

  @SneakyThrows
  default void beginTransaction()
  {
    if (getEntityManager().getTransaction().isActive())
    {
      return;
    }
    getEntityManager().getTransaction().begin();
  }

  default void commitTransaction()
  {
    if (getEntityManager().getTransaction().isActive())
    {
      getEntityManager().getTransaction().commit();
    }
  }

}
