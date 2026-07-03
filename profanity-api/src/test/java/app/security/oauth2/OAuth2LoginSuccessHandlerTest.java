package app.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class OAuth2LoginSuccessHandlerTest {

  private static final SsoFrontendProperties FRONTEND_PROPERTIES =
      new SsoFrontendProperties("http://localhost:5173/login");

  @Test
  @DisplayName("GitHub 로그인 성공 시 mock dashboard token과 사용자 정보를 FE fragment로 redirect한다")
  void onAuthenticationSuccess_whenGithubLoginSucceeded_redirectsToFrontendFragment()
      throws Exception {
    OAuth2LoginSuccessHandler successHandler = new OAuth2LoginSuccessHandler(FRONTEND_PROPERTIES);
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            Map.of(
                "id", 12345,
                "login", "hgkim",
                "avatar_url", "https://avatars.githubusercontent.com/u/12345"),
            "id");
    OAuth2AuthenticationToken authentication =
        new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), "github");
    MockHttpServletResponse response = new MockHttpServletResponse();

    successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
    assertThat(response.getRedirectedUrl())
        .startsWith("http://localhost:5173/login#")
        .contains("provider=github")
        .contains("providerUserId=12345")
        .contains("providerLogin=hgkim")
        .contains("githubUserId=12345")
        .contains("githubLogin=hgkim")
        .contains("dashboardAccessToken=mock_dashboard_token_");
  }

  @Test
  @DisplayName("GitHub 사용자 속성이 비어 있어도 null 없이 FE fragment로 redirect한다")
  void onAuthenticationSuccess_whenGithubAttributesAreMissing_redirectsWithEmptyStrings()
      throws Exception {
    OAuth2LoginSuccessHandler successHandler = new OAuth2LoginSuccessHandler(FRONTEND_PROPERTIES);
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("id", 12345), "id");
    OAuth2AuthenticationToken authentication =
        new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), "github");
    MockHttpServletResponse response = new MockHttpServletResponse();

    successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
    assertThat(response.getRedirectedUrl())
        .contains("providerUserId=12345")
        .contains("providerLogin=")
        .contains("githubUserId=12345")
        .contains("githubLogin=")
        .contains("dashboardAccessToken=mock_dashboard_token_");
  }

  @Test
  @DisplayName("Google 로그인 성공 시 mock dashboard token과 사용자 정보를 FE fragment로 redirect한다")
  void onAuthenticationSuccess_whenGoogleLoginSucceeded_redirectsToFrontendFragment()
      throws Exception {
    OAuth2LoginSuccessHandler successHandler = new OAuth2LoginSuccessHandler(FRONTEND_PROPERTIES);
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            Map.of(
                "sub", "google-user-123",
                "email", "hgkim@example.com",
                "name", "HG Kim",
                "picture", "https://example.com/profile.png"),
            "sub");
    OAuth2AuthenticationToken authentication =
        new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), "google");
    MockHttpServletResponse response = new MockHttpServletResponse();

    successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
    assertThat(response.getRedirectedUrl())
        .startsWith("http://localhost:5173/login#")
        .contains("provider=google")
        .contains("providerUserId=google-user-123")
        .contains("providerLogin=HG%20Kim")
        .contains("providerEmail=hgkim%40example.com")
        .contains("googleUserId=google-user-123")
        .contains("googleEmail=hgkim%40example.com")
        .contains("dashboardAccessToken=mock_dashboard_token_");
  }
}
