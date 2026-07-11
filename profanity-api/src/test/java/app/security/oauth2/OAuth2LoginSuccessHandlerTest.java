package app.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import app.application.auth.SsoLoginCompletionService;
import app.domain.user.OAuthLoginProfile;
import app.domain.user.OAuthProvider;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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
  @DisplayName("GitHub 로그인 성공 시 일회용 코드만 FE fragment로 redirect한다")
  void onAuthenticationSuccess_whenGithubLoginSucceeded_redirectsWithExchangeCodeOnly()
      throws Exception {
    AtomicReference<OAuthLoginProfile> capturedProfile = new AtomicReference<>();
    OAuth2LoginSuccessHandler successHandler = handler(capturedProfile);
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
        .isEqualTo("http://localhost:5173/login#code=one-time-code")
        .doesNotContain("providerUserId", "providerEmail", "accessToken", "refreshToken");
    assertThat(response.getHeader("Cache-Control")).contains("no-store");
    assertThat(capturedProfile.get().provider()).isEqualTo(OAuthProvider.GITHUB);
    assertThat(capturedProfile.get().providerUserId()).isEqualTo("12345");
    assertThat(capturedProfile.get().providerUsername()).isEqualTo("hgkim");
    assertThat(capturedProfile.get().emailVerified()).isFalse();
  }

  @Test
  @DisplayName("GitHub 선택 속성이 비어 있으면 provider id를 표시 이름으로 사용한다")
  void onAuthenticationSuccess_whenGithubOptionalAttributesAreMissing_usesProviderId()
      throws Exception {
    AtomicReference<OAuthLoginProfile> capturedProfile = new AtomicReference<>();
    OAuth2LoginSuccessHandler successHandler = handler(capturedProfile);
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("id", 12345), "id");
    OAuth2AuthenticationToken authentication =
        new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), "github");

    successHandler.onAuthenticationSuccess(
        new MockHttpServletRequest(), new MockHttpServletResponse(), authentication);

    assertThat(capturedProfile.get().displayName()).isEqualTo("12345");
    assertThat(capturedProfile.get().providerEmail()).isNull();
  }

  @Test
  @DisplayName("Google 로그인 성공 시 검증된 이메일 정보를 내부 프로필로 전달한다")
  void onAuthenticationSuccess_whenGoogleLoginSucceeded_passesVerifiedProfile() throws Exception {
    AtomicReference<OAuthLoginProfile> capturedProfile = new AtomicReference<>();
    OAuth2LoginSuccessHandler successHandler = handler(capturedProfile);
    OAuth2User oauth2User =
        new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            Map.of(
                "sub", "google-user-123",
                "email", "hgkim@example.com",
                "email_verified", true,
                "name", "HG Kim",
                "picture", "https://example.com/profile.png"),
            "sub");
    OAuth2AuthenticationToken authentication =
        new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), "google");
    MockHttpServletResponse response = new MockHttpServletResponse();

    successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

    assertThat(response.getRedirectedUrl())
        .isEqualTo("http://localhost:5173/login#code=one-time-code");
    assertThat(capturedProfile.get().provider()).isEqualTo(OAuthProvider.GOOGLE);
    assertThat(capturedProfile.get().providerUserId()).isEqualTo("google-user-123");
    assertThat(capturedProfile.get().providerEmail()).isEqualTo("hgkim@example.com");
    assertThat(capturedProfile.get().emailVerified()).isTrue();
    assertThat(capturedProfile.get().displayName()).isEqualTo("HG Kim");
  }

  private OAuth2LoginSuccessHandler handler(AtomicReference<OAuthLoginProfile> capturedProfile) {
    SsoLoginCompletionService loginCompletion =
        profile -> {
          capturedProfile.set(profile);
          return "one-time-code";
        };
    return new OAuth2LoginSuccessHandler(FRONTEND_PROPERTIES, loginCompletion);
  }
}
