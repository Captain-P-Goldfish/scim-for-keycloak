package de.captaingoldfish.scim.sdk.keycloak.provider;

import java.util.Arrays;
import java.util.List;

import de.captaingoldfish.scim.sdk.keycloak.entities.ScimEmailsEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 02.08.2020
 */
@Slf4j
public class ScimJpaEntityProvider implements JpaEntityProvider
{

  @Override
  public List<Class<?>> getEntities()
  {
    return Arrays.asList(ScimServiceProviderEntity.class,
                         ScimResourceTypeEntity.class,
                         ScimUserAttributesEntity.class,
                         ScimEmailsEntity.class);
  }

  @Override
  public String getChangelogLocation()
  {
    return "META-INF/scim-changelog.xml";
  }

  @Override
  public String getFactoryId()
  {
    return ScimJpaEntityProviderFactory.ID;
  }

  @Override
  public void close()
  {

  }
}
