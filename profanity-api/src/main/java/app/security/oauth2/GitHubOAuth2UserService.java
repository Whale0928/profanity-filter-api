package app.security.oauth2;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/** GitHub 기본 프로필에 검증된 대표 이메일을 보강합니다. */
public class GitHubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
  private static final String GITHUB_REGISTRATION_ID = "github";
  private static final String INVALID_USER_INFO_RESPONSE = "invalid_user_info_response";
  private static final MediaType GITHUB_JSON =
      MediaType.parseMediaType("application/vnd.github+json");

  private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
  private final RestClient restClient;

  public GitHubOAuth2UserService() {
    this(new DefaultOAuth2UserService(), RestClient.create());
  }

  GitHubOAuth2UserService(
      OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate, RestClient restClient) {
    this.delegate = delegate;
    this.restClient = restClient;
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User user = delegate.loadUser(userRequest);
    if (!GITHUB_REGISTRATION_ID.equals(userRequest.getClientRegistration().getRegistrationId())) {
      return user;
    }

    String verifiedPrimaryEmail = loadVerifiedPrimaryEmail(userRequest);
    Map<String, Object> attributes = new LinkedHashMap<>(user.getAttributes());
    attributes.put("email", verifiedPrimaryEmail);
    attributes.put("email_verified", true);
    String userNameAttributeName =
        userRequest
            .getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();
    return new DefaultOAuth2User(user.getAuthorities(), attributes, userNameAttributeName);
  }

  private String loadVerifiedPrimaryEmail(OAuth2UserRequest userRequest) {
    GitHubEmail[] emails;
    try {
      emails =
          restClient
              .get()
              .uri(emailEndpoint(userRequest))
              .headers(
                  headers -> headers.setBearerAuth(userRequest.getAccessToken().getTokenValue()))
              .accept(GITHUB_JSON)
              .retrieve()
              .body(GitHubEmail[].class);
    } catch (RestClientException | IllegalArgumentException exception) {
      throw invalidUserInfo("Failed to obtain GitHub email information", exception);
    }

    if (emails == null) {
      throw invalidUserInfo("GitHub verified primary email is unavailable");
    }
    return Arrays.stream(emails)
        .filter(GitHubEmail::primary)
        .filter(GitHubEmail::verified)
        .map(GitHubEmail::email)
        .filter(email -> email != null && !email.isBlank())
        .map(String::trim)
        .findFirst()
        .orElseThrow(() -> invalidUserInfo("GitHub verified primary email is unavailable"));
  }

  private URI emailEndpoint(OAuth2UserRequest userRequest) {
    URI userInfoEndpoint =
        URI.create(
            userRequest
                .getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUri());
    String path = userInfoEndpoint.getPath();
    String normalizedPath =
        path == null || path.isBlank()
            ? ""
            : path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    return UriComponentsBuilder.fromUri(userInfoEndpoint)
        .replacePath(normalizedPath + "/emails")
        .replaceQuery(null)
        .fragment(null)
        .build(true)
        .toUri();
  }

  private OAuth2AuthenticationException invalidUserInfo(String description) {
    OAuth2Error error = new OAuth2Error(INVALID_USER_INFO_RESPONSE, description, null);
    return new OAuth2AuthenticationException(error, error.toString());
  }

  private OAuth2AuthenticationException invalidUserInfo(String description, Throwable cause) {
    OAuth2Error error = new OAuth2Error(INVALID_USER_INFO_RESPONSE, description, null);
    return new OAuth2AuthenticationException(error, error.toString(), cause);
  }

  private record GitHubEmail(String email, boolean primary, boolean verified) {}
}
