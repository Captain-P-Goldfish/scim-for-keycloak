package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.custom.resources.ChildRole;
import de.captaingoldfish.scim.sdk.keycloak.custom.resources.RealmRole;
import de.captaingoldfish.scim.sdk.keycloak.custom.resources.RoleAssociate;
import de.captaingoldfish.scim.sdk.keycloak.scim.AbstractScimEndpointTest;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 16.08.2020
 */
@Slf4j
public class RealmRoleHandlerTest extends AbstractScimEndpointTest
{

  public static final String ROLES_ENDPOINT = "/RealmRoles";

  /**
   * checks that creating a role without any associates works as expected
   */
  @Test
  public void testCreateRoleWithoutAssociates()
  {
    final String roleName = "admin";
    final String description = "admin role";
    RealmRole role = RealmRole.builder().name(roleName).description(description).build();

    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .endpoint(ROLES_ENDPOINT)
                                               .method(HttpMethod.POST)
                                               .requestBody(role.toString())
                                               .build();
    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());
    RealmRole createdRole = JsonHelper.readJsonDocument((String)response.getEntity(), RealmRole.class);
    Assertions.assertEquals(roleName, createdRole.getName());
    Assertions.assertEquals(description, createdRole.getDescription().get());
  }

  /**
   * checks that a role can be created with some associated members
   */
  @Test
  public void testCreateRoleWithAssociates()
  {
    UserModel donkeyKong = getKeycloakSession().users().addUser(getRealmModel(), "DonkeyKong");
    UserModel superMario = getKeycloakSession().users().addUser(getRealmModel(), "SuperMario");
    GroupModel bremen = getKeycloakSession().groups().createGroup(getRealmModel(), "bremen");
    GroupModel berlin = getKeycloakSession().groups().createGroup(getRealmModel(), "berlin");

    final String roleName = "admin";
    final String description = "admin role";
    List<RoleAssociate> associates = new ArrayList<>();
    associates.add(RealmRoleHandler.toAssociate(donkeyKong));
    associates.add(RealmRoleHandler.toAssociate(superMario));
    associates.add(RealmRoleHandler.toAssociate(bremen));
    associates.add(RealmRoleHandler.toAssociate(berlin));

    RealmRole role = RealmRole.builder().name(roleName).description(description).associates(associates).build();

    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .endpoint(ROLES_ENDPOINT)
                                               .method(HttpMethod.POST)
                                               .requestBody(role.toString())
                                               .build();
    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());

    // validate SCIM representation
    {
      RealmRole createdRole = JsonHelper.readJsonDocument((String)response.getEntity(), RealmRole.class);
      Assertions.assertEquals(roleName, createdRole.getName());
      Assertions.assertEquals(description, createdRole.getDescription().get());
      Assertions.assertEquals(associates.size(), createdRole.getAssociates().size());
      associates.forEach(associate -> {
        Assertions.assertTrue(createdRole.getAssociates()
                                         .stream()
                                         .anyMatch(a -> a.getValue().get().equals(associate.getValue().get())
                                                        && a.getType().get().equals(associate.getType().get())));
      });
    }

    // validate database
    {
      RoleModel roleModel = getKeycloakSession().roles().getRealmRole(getRealmModel(), roleName);
      Assertions.assertNotNull(roleModel);
      List<GroupModel> groupModels = getKeycloakSession().groups()
                                                         .getGroupsByRoleStream(getRealmModel(), roleModel, -1, -1)
                                                         .collect(Collectors.toList());
      Assertions.assertEquals(2, groupModels.size());
      Assertions.assertTrue(groupModels.stream().anyMatch(group -> group.getName().equals(bremen.getName())));
      Assertions.assertTrue(groupModels.stream().anyMatch(group -> group.getName().equals(berlin.getName())));

      List<UserModel> userModels = getKeycloakSession().users()
                                                       .getRoleMembersStream(getRealmModel(), roleModel)
                                                       .collect(Collectors.toList());
      Assertions.assertEquals(2, userModels.size());
      Assertions.assertTrue(userModels.stream().anyMatch(user -> user.getUsername().equals(donkeyKong.getUsername())));
      Assertions.assertTrue(userModels.stream().anyMatch(user -> user.getUsername().equals(superMario.getUsername())));
    }
  }

  /**
   * checks that a composite role can be created
   */
  @Test
  public void testCreateRoleWithChildren()
  {
    RoleModel creator = getKeycloakSession().roles().addRealmRole(getRealmModel(), "creator");
    RoleModel destroyer = getKeycloakSession().roles().addRealmRole(getRealmModel(), "destroyer");

    ClientProvider clientProvider = getKeycloakSession().realms();
    ClientModel clientModel = clientProvider.addClient(getRealmModel(), "test-client");
    RoleModel testClientRole = getKeycloakSession().roles().addClientRole(clientModel, "test-client-role");

    final String roleName = "admin";
    final String description = "admin role";
    List<ChildRole> children = new ArrayList<>();
    children.add(RealmRoleHandler.toChildRole(creator));
    children.add(RealmRoleHandler.toChildRole(destroyer));
    children.add(RealmRoleHandler.toChildRole(testClientRole));

    RealmRole role = RealmRole.builder().name(roleName).description(description).children(children).build();

    HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                               .endpoint(ROLES_ENDPOINT)
                                               .method(HttpMethod.POST)
                                               .requestBody(role.toString())
                                               .build();
    Response response = getScimEndpoint().handleScimRequest(request);
    Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());


    // validate SCIM representation
    {
      RealmRole createdRole = JsonHelper.readJsonDocument((String)response.getEntity(), RealmRole.class);
      log.debug(createdRole.toPrettyString());

      Assertions.assertEquals(roleName, createdRole.getName());
      Assertions.assertEquals(description, createdRole.getDescription().get());
      Assertions.assertEquals(children.size(), createdRole.getChildren().size());
      children.forEach(child -> {
        Assertions.assertTrue(createdRole.getChildren()
                                         .stream()
                                         .anyMatch(a -> a.getValue().get().equals(child.getValue().get())
                                                        && a.getDisplay().get().equals(child.getDisplay().get())));
      });
    }

    // validate database
    {
      RoleModel roleModel = getKeycloakSession().roles().getRealmRole(getRealmModel(), roleName);
      Assertions.assertNotNull(roleModel);
      Assertions.assertEquals(3, roleModel.getCompositesStream().count());

      roleModel.getCompositesStream().forEach(composite -> {
        Assertions.assertTrue(children.stream()
                                      .anyMatch(child -> child.getDisplay().get().equals(composite.getName())));
      });
    }
  }

  /**
   * checks that a role can be updated
   */
  @Test
  public void testUpdateRole()
  {
    // prepare
    final String roleName = "admin";
    final String description = "admin role";

    UserModel donkeyKong = getKeycloakSession().users().addUser(getRealmModel(), "DonkeyKong");
    UserModel superMario = getKeycloakSession().users().addUser(getRealmModel(), "SuperMario");
    GroupModel bremen = getKeycloakSession().groups().createGroup(getRealmModel(), "bremen");
    GroupModel berlin = getKeycloakSession().groups().createGroup(getRealmModel(), "berlin");

    RoleModel creator = getKeycloakSession().roles().addRealmRole(getRealmModel(), "creator");
    RoleModel destroyer = getKeycloakSession().roles().addRealmRole(getRealmModel(), "destroyer");

    ClientProvider clientProvider = getKeycloakSession().realms();
    ClientModel clientModel = clientProvider.addClient(getRealmModel(), "test-client");
    RoleModel testClientRole = getKeycloakSession().roles().addClientRole(clientModel, "test-client-role");

    RealmRole createdRole;
    // create the role
    {

      List<RoleAssociate> associates = new ArrayList<>();
      associates.add(RealmRoleHandler.toAssociate(donkeyKong));
      associates.add(RealmRoleHandler.toAssociate(superMario));
      associates.add(RealmRoleHandler.toAssociate(bremen));
      associates.add(RealmRoleHandler.toAssociate(berlin));


      List<ChildRole> children = new ArrayList<>();
      children.add(RealmRoleHandler.toChildRole(creator));
      children.add(RealmRoleHandler.toChildRole(destroyer));
      children.add(RealmRoleHandler.toChildRole(testClientRole));

      RealmRole role = RealmRole.builder()
                                .name(roleName)
                                .description(description)
                                .associates(associates)
                                .children(children)
                                .build();

      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                                 .endpoint(ROLES_ENDPOINT)
                                                 .method(HttpMethod.POST)
                                                 .requestBody(role.toString())
                                                 .build();
      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());
      createdRole = JsonHelper.readJsonDocument((String)response.getEntity(), RealmRole.class);
    }

    // update preparation
    final String newDescription = "the new description";
    UserModel peach = getKeycloakSession().users().addUser(getRealmModel(), "Peach");
    RoleModel princess = getKeycloakSession().roles().addRealmRole(getRealmModel(), "princess");

    RealmRole updatedRole;
    // update the role
    {
      List<RoleAssociate> associates = new ArrayList<>(createdRole.getAssociates());
      associates.removeIf(associate -> associate.getDisplay().get().equals(donkeyKong.getUsername())
                                       || associate.getDisplay().get().equals(berlin.getName()));
      associates.add(RealmRoleHandler.toAssociate(peach));

      List<ChildRole> children = new ArrayList<>(createdRole.getChildren());
      children.removeIf(role -> role.getDisplay().get().equals(testClientRole.getName()));
      children.add(RealmRoleHandler.toChildRole(princess));

      RealmRole roleToUpdate = RealmRole.builder()
                                        .name(createdRole.getName())
                                        .description(newDescription)
                                        .associates(associates)
                                        .children(children)
                                        .build();

      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                                 .endpoint(ROLES_ENDPOINT + "/" + createdRole.getId().get())
                                                 .method(HttpMethod.PUT)
                                                 .requestBody(roleToUpdate.toString())
                                                 .build();
      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.OK, response.getStatus());
      updatedRole = JsonHelper.readJsonDocument((String)response.getEntity(), RealmRole.class);
    }

    log.debug(updatedRole.toPrettyString());

    // validate updated returned role
    {
      Assertions.assertEquals(newDescription, updatedRole.getDescription().get());
      Assertions.assertEquals(3, updatedRole.getAssociates().size());
      Stream.of(peach, superMario).forEach(user -> {
        Assertions.assertTrue(updatedRole.getAssociates()
                                         .stream()
                                         .anyMatch(associate -> associate.getValue().get().equals(user.getId())
                                                                && associate.getDisplay()
                                                                            .get()
                                                                            .equals(user.getUsername())));
      });
      Stream.of(bremen).forEach(group -> {
        Assertions.assertTrue(updatedRole.getAssociates()
                                         .stream()
                                         .anyMatch(associate -> associate.getValue().get().equals(group.getId())
                                                                && associate.getDisplay()
                                                                            .get()
                                                                            .equals(group.getName())));
      });

      Assertions.assertEquals(3, updatedRole.getChildren().size());
      Stream.of(princess, creator, destroyer).forEach(role -> {
        Assertions.assertTrue(updatedRole.getChildren()
                                         .stream()
                                         .anyMatch(children -> children.getValue().get().equals(role.getId())
                                                               && children.getDisplay().get().equals(role.getName())));
      });
    }

    // validate database
    {
      RoleModel adminRole = getKeycloakSession().roles().getRealmRole(getRealmModel(), roleName);
      Assertions.assertEquals(updatedRole.getChildren().size(), adminRole.getCompositesStream().count());
      updatedRole.getChildren().forEach(child -> {
        Assertions.assertTrue(adminRole.getCompositesStream()
                                       .anyMatch(composite -> child.getValue().get().equals(composite.getId())
                                                              && child.getDisplay().get().equals(composite.getName())));
      });

      List<GroupModel> groupModels = getKeycloakSession().groups()
                                                         .getGroupsByRoleStream(getRealmModel(), adminRole, -1, -1)
                                                         .collect(Collectors.toList());
      Assertions.assertEquals(1, groupModels.size());
      Assertions.assertTrue(groupModels.stream().anyMatch(group -> group.getName().equals(bremen.getName())));

      List<UserModel> userModels = getKeycloakSession().users()
                                                       .getRoleMembersStream(getRealmModel(), adminRole)
                                                       .collect(Collectors.toList());
      Assertions.assertEquals(2, userModels.size());
      Assertions.assertTrue(userModels.stream().anyMatch(user -> user.getUsername().equals(peach.getUsername())));
      Assertions.assertTrue(userModels.stream().anyMatch(user -> user.getUsername().equals(superMario.getUsername())));
    }
  }

  /**
   * verifies that a role is successfully deleted even if it has several mappings to other resources
   */
  @Test
  public void testDeleteRole()
  {
    // prepare
    final String roleName = "admin";
    final String description = "admin role";

    UserModel donkeyKong = getKeycloakSession().users().addUser(getRealmModel(), "DonkeyKong");
    UserModel superMario = getKeycloakSession().users().addUser(getRealmModel(), "SuperMario");
    GroupModel bremen = getKeycloakSession().groups().createGroup(getRealmModel(), "bremen");
    GroupModel berlin = getKeycloakSession().groups().createGroup(getRealmModel(), "berlin");

    RoleModel creator = getKeycloakSession().roles().addRealmRole(getRealmModel(), "creator");
    RoleModel destroyer = getKeycloakSession().roles().addRealmRole(getRealmModel(), "destroyer");

    ClientProvider clientProvider = getKeycloakSession().realms();
    ClientModel clientModel = clientProvider.addClient(getRealmModel(), "test-client");
    RoleModel testClientRole = getKeycloakSession().roles().addClientRole(clientModel, "test-client-role");

    RealmRole createdRole;
    // create the role
    {

      List<RoleAssociate> associates = new ArrayList<>();
      associates.add(RealmRoleHandler.toAssociate(donkeyKong));
      associates.add(RealmRoleHandler.toAssociate(superMario));
      associates.add(RealmRoleHandler.toAssociate(bremen));
      associates.add(RealmRoleHandler.toAssociate(berlin));


      List<ChildRole> children = new ArrayList<>();
      children.add(RealmRoleHandler.toChildRole(creator));
      children.add(RealmRoleHandler.toChildRole(destroyer));
      children.add(RealmRoleHandler.toChildRole(testClientRole));

      RealmRole role = RealmRole.builder()
                                .name(roleName)
                                .description(description)
                                .associates(associates)
                                .children(children)
                                .build();

      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                                 .endpoint(ROLES_ENDPOINT)
                                                 .method(HttpMethod.POST)
                                                 .requestBody(role.toString())
                                                 .build();
      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.CREATED, response.getStatus());
      createdRole = JsonHelper.readJsonDocument((String)response.getEntity(), RealmRole.class);
    }

    // delete role
    {
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                                 .endpoint(ROLES_ENDPOINT + "/" + createdRole.getId().get())
                                                 .method(HttpMethod.DELETE)
                                                 .build();
      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatus());
    }

    // validate database
    {
      RoleModel adminRole = getKeycloakSession().roles().getRealmRole(getRealmModel(), roleName);
      Assertions.assertNull(adminRole);
    }
  }
}
