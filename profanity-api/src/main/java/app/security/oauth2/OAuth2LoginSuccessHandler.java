package app.security.oauth2;

import app.application.auth.SsoLoginCompletionService;
import app.domain.user.OAuthLoginProfile;
import app.domain.user.OAuthProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
  private final SsoFrontendProperties ssoFrontendProperties;
  private final SsoLoginCompletionService loginCompletionService;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    if (!(authentication instanceof OAuth2AuthenticationToken oauthAuthentication)
        || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
      throw new IllegalArgumentException("OAuth2 login authentication is required");
    }

    OAuthProvider provider =
        OAuthProvider.from(oauthAuthentication.getAuthorizedClientRegistrationId());
    OAuthLoginProfile profile = providerProfile(provider, oauth2User);
    String exchangeCode = loginCompletionService.issueExchangeCode(profile);

    log.info("OAuth2 login succeeded. provider={}", provider.value());
    response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
    response.setHeader(HttpHeaders.PRAGMA, "no-cache");
    response.sendRedirect(ssoFrontendProperties.redirectUri() + "#code=" + encode(exchangeCode));
  }

  private OAuthLoginProfile providerProfile(OAuthProvider provider, OAuth2User oauth2User) {
    return switch (provider) {
      case GITHUB -> githubProfile(oauth2User);
      case GOOGLE -> googleProfile(oauth2User);
    };
  }

  private OAuthLoginProfile githubProfile(OAuth2User user) {
    String providerUserId = requiredAttribute(user, "id");
    String username = attribute(user, "login");
    String email = attribute(user, "email");
    String displayName = firstNonBlank(attribute(user, "name"), username, providerUserId);
    return new OAuthLoginProfile(
        OAuthProvider.GITHUB,
        providerUserId,
        email,
        false,
        username,
        displayName,
        attribute(user, "avatar_url"));
  }

  private OAuthLoginProfile googleProfile(OAuth2User user) {
    String providerUserId = requiredAttribute(user, "sub");
    String email = attribute(user, "email");
    String displayName = firstNonBlank(attribute(user, "name"), email, providerUserId);
    return new OAuthLoginProfile(
        OAuthProvider.GOOGLE,
        providerUserId,
        email,
        Boolean.TRUE.equals(user.getAttribute("email_verified")),
        email,
        displayName,
        attribute(user, "picture"));
  }

  private String requiredAttribute(OAuth2User user, String name) {
    String value = attribute(user, name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("OAuth2 provider identity is missing");
    }
    return value;
  }

  private String attribute(OAuth2User user, String name) {
    Object value = user.getAttribute(name);
    return value == null ? null : String.valueOf(value);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    throw new IllegalArgumentException("OAuth2 display name is missing");
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
