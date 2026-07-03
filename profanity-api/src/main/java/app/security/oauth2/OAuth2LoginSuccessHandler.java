package app.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private static final String MOCK_TOKEN_PREFIX = "mock_dashboard_token_";

  private final SsoFrontendProperties ssoFrontendProperties;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
    String provider = registrationId(authentication);
    String mockToken = MOCK_TOKEN_PREFIX + UUID.randomUUID().toString().replace("-", "");
    ProviderProfile providerProfile = providerProfile(provider, oauth2User);

    log.info(
        "OAuth2 login succeeded. provider={}, providerUserId={}, providerLogin={}, providerEmail={}",
        provider,
        providerProfile.userId(),
        providerProfile.login(),
        providerProfile.email());

    response.sendRedirect(
        ssoFrontendProperties.redirectUri()
            + "#provider="
            + encode(provider)
            + "&providerUserId="
            + encode(providerProfile.userId())
            + "&providerLogin="
            + encode(providerProfile.login())
            + "&providerEmail="
            + encode(providerProfile.email())
            + "&githubUserId="
            + encode(providerProfile.githubUserId())
            + "&githubLogin="
            + encode(providerProfile.githubLogin())
            + "&googleUserId="
            + encode(providerProfile.googleUserId())
            + "&googleEmail="
            + encode(providerProfile.googleEmail())
            + "&dashboardAccessToken="
            + encode(mockToken));
  }

  private ProviderProfile providerProfile(String provider, OAuth2User oauth2User) {
    return switch (provider) {
      case "github" ->
          new ProviderProfile(
              attributeAsString(oauth2User, "id"),
              attributeAsString(oauth2User, "login"),
              attributeAsString(oauth2User, "email"),
              attributeAsString(oauth2User, "id"),
              attributeAsString(oauth2User, "login"),
              "",
              "");
      case "google" ->
          new ProviderProfile(
              attributeAsString(oauth2User, "sub"),
              attributeAsString(oauth2User, "name"),
              attributeAsString(oauth2User, "email"),
              "",
              "",
              attributeAsString(oauth2User, "sub"),
              attributeAsString(oauth2User, "email"));
      default -> new ProviderProfile("", "", "", "", "", "", "");
    };
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private String attributeAsString(OAuth2User oauth2User, String attributeName) {
    Object attribute = oauth2User.getAttribute(attributeName);
    if (attribute == null) {
      return "";
    }
    return String.valueOf(attribute);
  }

  private String registrationId(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken) {
      return oauth2AuthenticationToken.getAuthorizedClientRegistrationId();
    }
    return "unknown";
  }

  private record ProviderProfile(
      String userId,
      String login,
      String email,
      String githubUserId,
      String githubLogin,
      String googleUserId,
      String googleEmail) {}
}
