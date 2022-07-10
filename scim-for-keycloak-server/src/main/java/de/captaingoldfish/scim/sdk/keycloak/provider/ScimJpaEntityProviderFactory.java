package de.captaingoldfish.scim.sdk.keycloak.provider;

import org.keycloak.Config;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;

import de.captaingoldfish.scim.sdk.keycloak.scim.ScimConfiguration;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimResourceTypeService;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimServiceProviderService;


/**
 * @author Pascal Knueppel
 * @since 02.08.2020
 */
public class ScimJpaEntityProviderFactory implements JpaEntityProviderFactory
{

  public static final String ID = "scim-jpa-entity-provider";

  @Override
  public JpaEntityProvider create(KeycloakSession session)
  {
    return new ScimJpaEntityProvider();
  }

  @Override
  public void init(Config.Scope config)
  {

  }

  @Override
  public void postInit(KeycloakSessionFactory factory)
  {
    factory.register((event) -> {
      if (event instanceof RealmModel.RealmRemovedEvent)
      {
        realmRemoved(((RealmModel.RealmRemovedEvent)event).getKeycloakSession());
      }
      else if (event instanceof RoleContainerModel.RoleRemovedEvent)
      {
        RoleContainerModel.RoleRemovedEvent roleRemovedEvent = (RoleContainerModel.RoleRemovedEvent)event;
        KeycloakSession keycloakSession = roleRemovedEvent.getKeycloakSession();
        RoleModel roleModel = roleRemovedEvent.getRole();
        roleRemoved(keycloakSession, roleModel);
      }
      else if (event instanceof ClientModel.ClientRemovedEvent)
      {
        ClientModel.ClientRemovedEvent clientRemovedEvent = (ClientModel.ClientRemovedEvent)event;
        KeycloakSession keycloakSession = clientRemovedEvent.getKeycloakSession();
        ClientModel clientModel = clientRemovedEvent.getClient();
        clientRemoved(keycloakSession, clientModel);
      }
    });
  }

  @Override
  public void close()
  {

  }

  @Override
  public String getId()
  {
    return ID;
  }

  /**
   * calls the services and removes the setups for the deleted realms
   */
  public void realmRemoved(KeycloakSession keycloakSession)
  {
    new ScimServiceProviderService(keycloakSession).deleteProvider();
    new ScimResourceTypeService(keycloakSession).deleteResourceTypes();
    ScimConfiguration.realmRemoved(keycloakSession);
  }

  /**
   * if a role was removed that was associated with a scim endpoint it must also be removed from the scim
   * database configuration
   * 
   * @param removedRole the role that was removed
   */
  public void roleRemoved(KeycloakSession keycloakSession, RoleModel removedRole)
  {
    new ScimResourceTypeService(keycloakSession).removeAssociatedRoles(removedRole);
  }

  /**
   * if a client was removed that was associated with a scim service provider it must also be removed from the
   * scim database configuration
   * 
   * @param removedClient the client that was removed
   */
  public void clientRemoved(KeycloakSession keycloakSession, ClientModel removedClient)
  {
    new ScimServiceProviderService(keycloakSession).removeAssociatedClients(removedClient);
  }
}
