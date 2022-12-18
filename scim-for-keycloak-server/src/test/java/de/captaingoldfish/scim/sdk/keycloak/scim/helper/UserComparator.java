package de.captaingoldfish.scim.sdk.keycloak.scim.helper;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.keycloak.scim.resources.CustomUser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * @author Pascal Knueppel
 * @since 18.12.2022
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserComparator
{

  /**
   * this method will check that users returned from the SCIM endpoint do contain the expected dataset if a
   * filter response was received
   */
  public static void checkUserEquality(CustomUser expectedUser, CustomUser actualUser)
  {
    Assertions.assertEquals(expectedUser.getUserName().orElse(null), actualUser.getUserName().orElse(null));
    Assertions.assertEquals(expectedUser.getExternalId().orElse(null), actualUser.getExternalId().orElse(null));
    Assertions.assertEquals(expectedUser.getName().flatMap(Name::getFormatted).orElse(null),
                            actualUser.getName().flatMap(Name::getFormatted).orElse(null));
    Assertions.assertEquals(expectedUser.getName().flatMap(Name::getGivenName).orElse(null),
                            actualUser.getName().flatMap(Name::getGivenName).orElse(null));
    Assertions.assertEquals(expectedUser.getName().flatMap(Name::getMiddleName).orElse(null),
                            actualUser.getName().flatMap(Name::getMiddleName).orElse(null));
    Assertions.assertEquals(expectedUser.getName().flatMap(Name::getFamilyName).orElse(null),
                            actualUser.getName().flatMap(Name::getFamilyName).orElse(null));
    Assertions.assertEquals(expectedUser.getName().flatMap(Name::getHonorificPrefix).orElse(null),
                            actualUser.getName().flatMap(Name::getHonorificPrefix).orElse(null));
    Assertions.assertEquals(expectedUser.getName().flatMap(Name::getHonorificSuffix).orElse(null),
                            actualUser.getName().flatMap(Name::getHonorificSuffix).orElse(null));
    Assertions.assertEquals(expectedUser.getDisplayName().orElse(null), actualUser.getDisplayName().orElse(null));
    Assertions.assertEquals(expectedUser.getNickName().orElse(null), actualUser.getNickName().orElse(null));
    Assertions.assertEquals(expectedUser.getProfileUrl().orElse(null), actualUser.getProfileUrl().orElse(null));
    Assertions.assertEquals(expectedUser.getUserType().orElse(null), actualUser.getUserType().orElse(null));
    Assertions.assertEquals(expectedUser.getPreferredLanguage().orElse(null),
                            actualUser.getPreferredLanguage().orElse(null));
    Assertions.assertEquals(expectedUser.getLocale().orElse(null), actualUser.getLocale().orElse(null));
    Assertions.assertEquals(expectedUser.getTimezone().orElse(null), actualUser.getTimezone().orElse(null));
    Assertions.assertEquals(expectedUser.isActive().orElse(null), actualUser.isActive().orElse(null));

    UserComparator.checkMultivaluedComplexEquality(expectedUser::getEmails, actualUser::getEmails);
    UserComparator.checkMultivaluedComplexEquality(expectedUser::getPhoneNumbers, actualUser::getPhoneNumbers);
    UserComparator.checkMultivaluedComplexEquality(expectedUser::getIms, actualUser::getIms);
    UserComparator.checkMultivaluedComplexEquality(expectedUser::getPhotos, actualUser::getPhotos);
    UserComparator.checkMultivaluedComplexEquality(expectedUser::getAddresses, actualUser::getAddresses);
    UserComparator.checkMultivaluedComplexEquality(expectedUser::getEntitlements, actualUser::getEntitlements);
    UserComparator.checkMultivaluedComplexEquality(expectedUser::getX509Certificates, actualUser::getX509Certificates);

    Assertions.assertEquals(expectedUser.getCountryUserExtension(), actualUser.getCountryUserExtension());
  }

  public static <T extends JsonNode> void checkMultivaluedComplexEquality(Supplier<List<T>> supplier1,
                                                                          Supplier<List<T>> supplier2)
  {
    Assertions.assertEquals(supplier1.get().size(),
                            supplier2.get().size(),
                            supplier2.get().stream().map(JsonNode::toPrettyString).collect(Collectors.joining("\n")));
    Assertions.assertTrue(supplier1.get().stream().allMatch(expectedItem -> {
      return supplier2.get().stream().anyMatch(actualItem -> actualItem.equals(expectedItem));
    }));
  }
}
