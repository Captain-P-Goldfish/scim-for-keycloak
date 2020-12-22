package de.captaingoldfish.scim.sdk.keycloak.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import org.keycloak.Config;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;

import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import de.captaingoldfish.scim.sdk.keycloak.provider.RealmRoleInitializer;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimServiceProviderService;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 04.02.2020 <br>
 * <br>
 */
@Slf4j
public class Authentication
{

  private static final String ERROR_MESSAGE_AUTHENTICATION_FAILED = "Authentication failed";

  /**
   * Authenticates the calling user and client according to the Bearer Token in the HTTP header.
   *
   * @return authentication result object
   * @throws ClientErrorException on authentication errors
   */
  public AdminAuth authenticate(KeycloakSession keycloakSession)
  {
    KeycloakContext context = keycloakSession.getContext();
    String accessToken = AppAuthManager.extractAuthorizationHeaderToken(context.getRequestHeaders());
    if (accessToken == null)
    {
      log.error(ERROR_MESSAGE_AUTHENTICATION_FAILED);
      throw new NotAuthorizedException(ERROR_MESSAGE_AUTHENTICATION_FAILED);
    }
    RealmModel authenticationRealm = keycloakSession.getContext().getRealm();
    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(keycloakSession);
    AuthenticationManager.AuthResult authResult = authenticator.setRealm(authenticationRealm)
                                                               .setConnection(context.getConnection())
                                                               .setHeaders(context.getRequestHeaders())
                                                               .authenticate();
    if (authResult == null)
    {
      log.error(ERROR_MESSAGE_AUTHENTICATION_FAILED);
      throw new NotAuthorizedException(ERROR_MESSAGE_AUTHENTICATION_FAILED);
    }
    AdminAuth adminAuth = createAdminAuth(keycloakSession, authResult);

    ScimServiceProviderService serviceProviderService = new ScimServiceProviderService(keycloakSession);
    Optional<ScimServiceProviderEntity> optionalEntity = serviceProviderService.getServiceProviderEntity();
    if (optionalEntity.isPresent())
    {
      // there might be an association to an existing client with this service provider. If so the associated
      // clients are the only ones that are allowed to access the SCIM endpoint
      ScimServiceProviderEntity entity = optionalEntity.get();
      boolean isClientAuthorized = entity.getAuthorizedClients().isEmpty()
                                   || entity.getAuthorizedClients()
                                            .stream()
                                            .map(ClientEntity::getClientId)
                                            .anyMatch(clientId -> clientId.equals(adminAuth.getClient().getClientId()));
      if (!isClientAuthorized)
      {
        log.error(ERROR_MESSAGE_AUTHENTICATION_FAILED);
        throw new NotAuthorizedException(ERROR_MESSAGE_AUTHENTICATION_FAILED);
      }
    }
    // if no service provider representation are found in the database (which shouldn't happen under normal
    // circumstances) we do not expect any clients to be associated with the current service provider
    return adminAuth;
  }

  /**
   * checks if the just logged in user was granted the {@link RealmRoleInitializer#SCIM_ADMIN_ROLE} to access
   * the SCIM administration
   * 
   * @param keycloakSession the current request context
   */
  public void authenticateAsScimAdmin(KeycloakSession keycloakSession)
  {
    AdminAuth adminAuth = authenticateOnRealm(keycloakSession);
    List<RoleModel> authorizedRoles = getAuthorizedRoles(keycloakSession);
    boolean accessGranted = authorizedRoles.stream()
                                           .anyMatch(authorizedRole -> adminAuth.getUser().hasRole(authorizedRole));
    if (!accessGranted)
    {
      throw new NotAuthorizedException(ERROR_MESSAGE_AUTHENTICATION_FAILED);
    }
  }

  /**
   * tries to get the roles that are authorized to access the SCIM environment
   * 
   * @param keycloakSession the current request context
   * @return should be the "scim-admin" role of the master client of this realm that is located in the master
   *         realm itself (used to grant access for the admin user) and the "scim-admin" role of the
   *         realm-management client of the current realm
   */
  private List<RoleModel> getAuthorizedRoles(KeycloakSession keycloakSession)
  {
    List<RoleModel> authorizedRoles = new ArrayList<>();
    RoleModel masterClientRoleModel = keycloakSession.getContext()
                                                     .getRealm()
                                                     .getMasterAdminClient()
                                                     .getRole(RealmRoleInitializer.SCIM_ADMIN_ROLE);
    authorizedRoles.add(masterClientRoleModel);
    RealmModel currentRealm = keycloakSession.getContext().getRealm();
    RealmManager realmManager = new RealmManager(keycloakSession);
    String realmManagementClientId;
    if (currentRealm.getId().equals("master"))
    {
      // could not find any matching constant in the keycloak project
      realmManagementClientId = "master-realm";
    }
    else
    {
      // is actually hardcoded by keycloak and is always "realm-management"
      realmManagementClientId = realmManager.getRealmAdminClientId(currentRealm);
    }
    ClientModel clientModel = currentRealm.getClientByClientId(realmManagementClientId);
    RoleModel realmClientRoleModel = clientModel.getRole(RealmRoleInitializer.SCIM_ADMIN_ROLE);
    authorizedRoles.add(realmClientRoleModel);
    return authorizedRoles;
  }

  /**
   * allows a user from a different realm to authenticate on the current realm. Lets assume you create a new
   * realm with name "SCIM". If you now try to update the SCIM-configuration in this realm you are doing this
   * probably with the "admin"-user from the master realm. But this user cannot authenticate on the "SCIM" realm
   * because it has no relation to it. So we need to execute the authentication on the "master" realm by
   * manipulating the current context.
   * 
   * @param keycloakSession the current request context
   * @return the authentication result of the user that tried to authenticate
   * @see <a href="https://github.com/dteleguin/beercloak">https://github.com/dteleguin/beercloak</a>
   */
  private AdminAuth authenticateOnRealm(KeycloakSession keycloakSession)
  {
    KeycloakContext context = keycloakSession.getContext();
    RealmModel originalRealm = context.getRealm();
    String tokenString = AppAuthManager.extractAuthorizationHeaderToken(context.getRequestHeaders());

    if (tokenString == null)
    {
      throw new NotAuthorizedException("Bearer");
    }

    AccessToken token;

    try
    {
      JWSInput input = new JWSInput(tokenString);
      token = input.readJsonContent(AccessToken.class);
    }
    catch (JWSInputException e)
    {
      throw new NotAuthorizedException("Bearer token format error");
    }

    String realmName = token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
    RealmManager realmManager = new RealmManager(keycloakSession);
    RealmModel authenticationRealm = realmManager.getRealmByName(realmName);

    if (authenticationRealm == null)
    {
      throw new NotAuthorizedException("Unknown realm in token");
    }
    context.setRealm(authenticationRealm);

    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(keycloakSession);
    AuthenticationManager.AuthResult authResult = authenticator.setRealm(authenticationRealm)
                                                               .setConnection(context.getConnection())
                                                               .setHeaders(context.getRequestHeaders())
                                                               .authenticate();
    if (authResult == null)
    {
      throw new NotAuthorizedException("Bearer");
    }
    context.setRealm(originalRealm);

    // @formatter:off
    ClientModel client 
      = authenticationRealm.getName().equals(Config.getAdminRealm()) 
      ? originalRealm.getMasterAdminClient()
      : originalRealm.getClientByClientId(realmManager.getRealmAdminClientId(originalRealm));
    // @formatter:on

    if (client == null)
    {
      throw new NotFoundException("Could not find client for authorization");
    }
    return createAdminAuth(keycloakSession, authResult);
  }

  /**
   * creates a valid authentication object for the user
   *
   * @param result the result of the users authentication
   * @return the authentication object for the user
   */
  private AdminAuth createAdminAuth(KeycloakSession keycloakSession, AuthenticationManager.AuthResult result)
  {
    KeycloakContext context = keycloakSession.getContext();
    RealmModel realm = context.getRealm();
    // authorized party ("azp" JWT claim)
    String authorizedParty = result.getToken().getIssuedFor();
    ClientModel client = realm.getClientByClientId(authorizedParty);
    if (client == null)
    {
      log.error(ERROR_MESSAGE_AUTHENTICATION_FAILED);
      throw new NotFoundException(ERROR_MESSAGE_AUTHENTICATION_FAILED);
    }
    AdminAuth adminAuth = new AdminAuth(realm, result.getToken(), result.getUser(), client);
    log.debug("user '{}' was successfully authenticated", adminAuth.getUser().getUsername());
    return adminAuth;
  }
}
