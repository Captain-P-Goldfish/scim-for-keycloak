package de.captaingoldfish.scim.sdk.keycloak.scim.resources;

import java.util.List;

import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 16.10.2021
 */
@NoArgsConstructor
public class CountryUserExtension extends ScimObjectNode
{

  @Builder
  public CountryUserExtension(List<String> countries, List<String> businessLine)
  {
    setCountries(countries);
    setBusinessLine(businessLine);
  }

  public List<String> getCountries()
  {
    return getSimpleArrayAttribute(FieldNames.COUNTRIES);
  }

  public void setCountries(List<String> country)
  {
    setStringAttributeList(FieldNames.COUNTRIES, country);
  }

  public List<String> getBusinessLine()
  {
    return getSimpleArrayAttribute(FieldNames.BUSINESS_LINE);
  }

  public void setBusinessLine(List<String> businessLine)
  {
    setStringAttributeList(FieldNames.BUSINESS_LINE, businessLine);
  }

  public static class FieldNames
  {

    public static final String COUNTRIES = "countries";

    public static final String BUSINESS_LINE = "businessLine";

  }
}
