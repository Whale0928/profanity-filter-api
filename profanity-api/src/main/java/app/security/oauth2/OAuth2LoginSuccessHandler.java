package app.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private static final String MOCK_TOKEN_PREFIX = "mock_dashboard_token_";
  private static final String FRONTEND_REDIRECT_URI =
      "http://localhost:63344/profanity-filter-api/sso/index.html";

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
    String mockToken = MOCK_TOKEN_PREFIX + UUID.randomUUID().toString().replace("-", "");
    String githubUserId = attributeAsString(oauth2User, "id");
    String githubLogin = attributeAsString(oauth2User, "login");

    log.info(
        "GitHub OAuth2 login succeeded. provider={}, githubUserId={}, githubLogin={}",
        registrationId(authentication),
        githubUserId,
        githubLogin);

    response.sendRedirect(
        FRONTEND_REDIRECT_URI
            + "#provider="
            + encode(registrationId(authentication))
            + "&githubUserId="
            + encode(githubUserId)
            + "&githubLogin="
            + encode(githubLogin)
            + "&dashboardAccessToken="
            + encode(mockToken));
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
}
