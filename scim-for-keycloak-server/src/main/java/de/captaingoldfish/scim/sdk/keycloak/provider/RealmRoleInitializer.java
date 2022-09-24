package de.captaingoldfish.scim.sdk.keycloak.provider;

import java.util.stream.Stream;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.keycloak.Config;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.services.managers.RealmManager;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * this class will initialize the role that is necessary to access and configure the scim configuration. This
 * class is inspired by beercloak:
 *
 * @author Pascal Knueppel
 * @since 01.08.2020 - 19:21
 * @see <a href=" https://github.com/dteleguin/beercloak">inspired by beercloak</a>
 */
@Slf4j
public class RealmRoleInitializer
{

  /**
   * the role that is required to access the SCIM configuration within the webadmin console
   */
  public static final String SCIM_ADMIN_ROLE = "scim-admin";

  /**
   * initializes the {@link #SCIM_ADMIN_ROLE} on all existing realms if not present and makes sure that the role
   * will also be added to all new created realms
   */
  @SneakyThrows
  public static void initializeRoles(KeycloakSessionFactory keycloakSessionFactory)
  {
    if (isHotDeploying())
    {
      log.info("initializing scim-sdk-server");
      try
      {
        KeycloakModelUtils.runJobInTransaction(keycloakSessionFactory, RealmRoleInitializer::setupRealmAccess);
      }
      catch (Exception ex)
      {
        log.error(ex.getMessage());
        final String wikiUrl = "https://github.com/Captain-P-Goldfish/scim-for-keycloak/wiki/Manually-fix:"
                               + "-Transaction-error-on-scim-sdk-server-database-initialization";
        log.warn("Transaction error on scim-sdk-server database initialization\n"
                 + "This is not necessarily a problem and can be ignored for most setups. See here '{}'. "
                 + "This bug was already reported at keycloak. But until it was fixed we are stuck with it.",
                 wikiUrl);
      }
    }
    else
    {
      log.debug("Server startup, waiting for PostMigrationEvent");
    }

    registerEventListener(keycloakSessionFactory);
  }

  /**
   * registers the event listener after deployment
   */
  protected static void registerEventListener(KeycloakSessionFactory keycloakSessionFactory)
  {
    keycloakSessionFactory.register((ProviderEvent event) -> {
      if (event instanceof RealmModel.RealmPostCreateEvent)
      {
        realmPostCreate((RealmModel.RealmPostCreateEvent)event);
      }
      else if (event instanceof PostMigrationEvent)
      {
        KeycloakModelUtils.runJobInTransaction(keycloakSessionFactory, RealmRoleInitializer::setupRealmAccess);
      }
    });
  }

  protected static boolean isHotDeploying()
  {
    /*
     * At the moment there's no standard way to determine if we are being cold or hot deployed. One of the ad-hoc
     * methods is to check for JNDI presence/absence. Another methods include querying current thread name and
     * RESTEasy features. See discussion: http://lists.jboss.org/pipermail/keycloak-dev/2017-July/009639.html
     */

    try
    {
      // JNDI present, we're invoked from an application thread, that means cold deployment
      new InitialContext().lookup("java:comp");
      return false;
    }
    catch (NamingException ex)
    {
      // JNDI absent, server thread, hot deployment
      return true;
    }
  }

  /**
   * gets the administration clients for all realms and grants access to configure the SCIM endpoints
   */
  private static void setupRealmAccess(KeycloakSession session)
  {
    Stream<RealmModel> realms = session.realms().getRealmsStream();
    RealmManager manager = new RealmManager(session);
    realms.forEach(realm -> {
      ClientModel client = realm.getMasterAdminClient();
      if (client.getRole(SCIM_ADMIN_ROLE) == null)
      {
        log.info("configuring realm {} for SCIM", realm.getName());
        addMasterAdminRoles(manager, realm);
      }

      if (!realm.getName().equals(Config.getAdminRealm()))
      {
        client = realm.getClientByClientId(manager.getRealmAdminClientId(realm));
        if (client.getRole(SCIM_ADMIN_ROLE) == null)
        {
          log.info("configuring realm {} for SCIM", realm.getName());
          addRealmAdminRoles(manager, realm);
        }
      }
    });
  }

  /**
   * is executed after a new realm is created and adds the {@link #SCIM_ADMIN_ROLE} to the newly created realm
   * to configure its own SCIM endpoint
   */
  private static void realmPostCreate(RealmModel.RealmPostCreateEvent event)
  {
    RealmModel realm = event.getCreatedRealm();
    RealmManager manager = new RealmManager(event.getKeycloakSession());
    addMasterAdminRoles(manager, realm);
    if (!realm.getName().equals(Config.getAdminRealm()))
    {
      addRealmAdminRoles(manager, realm);
    }
  }

  /**
   * adds the {@link #SCIM_ADMIN_ROLE} to the admin-user role of the 'master-realm' client
   */
  private static void addMasterAdminRoles(RealmManager manager, RealmModel realm)
  {
    RealmModel master = manager.getRealmByName(Config.getAdminRealm());
    RoleModel admin = master.getRole(AdminRoles.ADMIN);
    ClientModel client = realm.getMasterAdminClient();

    addScimAdminRole(client, admin);
  }

  /**
   * will add the role {@link #SCIM_ADMIN_ROLE} to the admin-user role of the given realm
   */
  private static void addRealmAdminRoles(RealmManager manager, RealmModel realm)
  {
    ClientModel client = realm.getClientByClientId(manager.getRealmAdminClientId(realm));
    RoleModel admin = client.getRole(AdminRoles.REALM_ADMIN);
    addScimAdminRole(client, admin);
  }

  /**
   * adds the {@link #SCIM_ADMIN_ROLE} to the given client and adds it as a composite role into the given
   * parent-role
   *
   * @param client the client to which the client-role {@link #SCIM_ADMIN_ROLE} should be added
   * @param parent the parent-role that will get the newly created {@link #SCIM_ADMIN_ROLE} as a composite role
   *          member
   */
  private static void addScimAdminRole(ClientModel client, RoleModel parent)
  {
    RoleModel role = client.addRole(SCIM_ADMIN_ROLE);
    log.info("created role '{}'", SCIM_ADMIN_ROLE);
    role.setDescription("${role_" + SCIM_ADMIN_ROLE + "}");
    parent.addCompositeRole(role);
    log.info("added role '{}' as composite member to role '{}'", SCIM_ADMIN_ROLE, parent.getName());
  }
}
