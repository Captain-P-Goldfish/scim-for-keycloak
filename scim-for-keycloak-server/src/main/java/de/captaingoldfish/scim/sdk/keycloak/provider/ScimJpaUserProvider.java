package de.captaingoldfish.scim.sdk.keycloak.provider;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaUserProvider;
import org.keycloak.models.jpa.entities.UserEntity;

import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
import lombok.extern.slf4j.Slf4j;


/**
 * this class will override the keycloak's default {@link JpaUserProvider} since we need to make sure that
 * users can be deleted by deleting the corresponding scim-tables first that have foreign-key references to
 * the {@link UserEntity} table
 * 
 * @author Pascal Knueppel
 * @since 17.12.2022
 */
@Slf4j
public class ScimJpaUserProvider extends JpaUserProvider
{

  private final KeycloakSession keycloakSession;

  public ScimJpaUserProvider(KeycloakSession keycloakSession, EntityManager em)
  {
    super(keycloakSession, em);
    this.keycloakSession = keycloakSession;
  }

  /**
   * a simple method that connects the {@link UserEntity} and the {@link ScimUserAttributesEntity} table with a
   * left join and returns the user as {@link ScimUserAttributesEntity} instance even if no entry within the
   * {@link ScimUserAttributesEntity}-table does exist
   */
  public static ScimUserAttributesEntity findUserById(KeycloakSession keycloakSession, String id)
  {
    EntityManager entityManager = keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();
    final String queryName = ScimUserAttributesEntity.GET_SCIM_USER_ATTRIBUTES_QUERY_NAME;
    Query query = entityManager.createNamedQuery(queryName);
    query.setParameter("userId", id);
    try
    {
      Object[] result = (Object[])query.getSingleResult();
      if (result[1] == null)
      {
        return ScimUserAttributesEntity.builder().userEntity((UserEntity)result[0]).build();
      }
      return (ScimUserAttributesEntity)result[1];
    }
    catch (NoResultException ex)
    {
      log.debug(ex.getMessage());
      // causes a 404 not found exception
      return null;
    }
  }

  /**
   * will remove the entry of the user from the {@link ScimUserAttributesEntity} table first and will then call
   * the standard keycloak delete-procedure. Since we have foreign key-references to the {@link UserEntity} we
   * need to remove these child-entries first.
   */
  @Override
  public boolean removeUser(RealmModel realm, UserModel user)
  {
    ScimUserAttributesEntity userAttributes = findUserById(keycloakSession, user.getId());
    if (userAttributes == null)
    {
      throw new ResourceNotFoundException(String.format("User with id '%s' does not exist", user.getId()));
    }
    if (userAttributes.getId() != null)
    {
      EntityManager entityManager = keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();
      entityManager.remove(userAttributes);
    }
    return super.removeUser(realm, user);
  }
}
