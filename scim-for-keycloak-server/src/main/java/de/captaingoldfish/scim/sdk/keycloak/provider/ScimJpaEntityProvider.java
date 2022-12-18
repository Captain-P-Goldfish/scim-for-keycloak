package de.captaingoldfish.scim.sdk.keycloak.provider;

import java.util.Arrays;
import java.util.List;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import de.captaingoldfish.scim.sdk.keycloak.entities.InfoCertBusinessLineEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.InfoCertCountriesEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimAddressEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimCertificatesEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimEmailsEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimEntitlementEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimImsEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimPersonRoleEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimPhonesEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimPhotosEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimUserAttributesEntity;
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
                         ScimAddressEntity.class,
                         ScimEmailsEntity.class,
                         ScimPhonesEntity.class,
                         ScimImsEntity.class,
                         ScimPhotosEntity.class,
                         ScimEntitlementEntity.class,
                         ScimCertificatesEntity.class,
                         ScimPersonRoleEntity.class,
                         InfoCertBusinessLineEntity.class,
                         InfoCertCountriesEntity.class

    );
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
