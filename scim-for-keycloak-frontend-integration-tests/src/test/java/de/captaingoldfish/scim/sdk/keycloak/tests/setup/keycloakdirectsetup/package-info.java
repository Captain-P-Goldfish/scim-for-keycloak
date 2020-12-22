/**
 * this package is used to grant a direct database access with a mocked keycloak setup that enables us to
 * check the changes on database level if something has changed on the web admin console<br>
 * <br>
 * <b>NOTE:</b><br>
 * This direct access setup has a big disadvantage. It works on the lower jpa layer of the keycloak and
 * doesn't affect the infinispan cache of the currently running keycloak which may lead to phantom reads
 * within the integration tests if not implemented properly
 * 
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
package de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup;
