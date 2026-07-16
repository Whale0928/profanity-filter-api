package app.security.filter;

import static app.core.data.response.constant.StatusCode.AMBIGUOUS_CREDENTIALS;
import static app.core.data.response.constant.StatusCode.LOGIN_TOKEN_INVALID;
import static app.core.data.response.constant.StatusCode.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.security.authentication.AuthenticationType;
import app.security.authentication.CredentialAuthenticationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

class RequestCredentialResolverTest {
  private final RequestCredentialResolver resolver =
      new RequestCredentialResolver(new AuthenticationRoutePolicy());

  @Test
  @DisplayName("외부 API의 API Key를 API_KEY 타입으로 해석한다")
  void resolve_externalApiWithApiKey_returnsApiKeyCredential() {
    MockHttpServletRequest request = externalApiRequest();
    request.addHeader(RequestCredentialResolver.API_KEY_HEADER, "test-api-key");

    RequestCredential credential = resolver.resolve(request);

    assertThat(credential.type()).isEqualTo(AuthenticationType.API_KEY);
    assertThat(credential.value()).isEqualTo("test-api-key");
    assertThat(credential.toString()).doesNotContain("test-api-key");
  }

  @Test
  @DisplayName("대시보드 Bearer token을 LOGIN_JWT 타입으로 해석한다")
  void resolve_dashboardWithBearer_returnsLoginJwtCredential() {
    MockHttpServletRequest request = request("GET", "/api/v1/auth/me");
    request.addHeader("Authorization", "Bearer login-token");

    RequestCredential credential = resolver.resolve(request);

    assertThat(credential.type()).isEqualTo(AuthenticationType.LOGIN_JWT);
    assertThat(credential.value()).isEqualTo("login-token");
  }

  @Test
  @DisplayName("외부 API의 Bearer token을 미구현 OAuth2 타입으로 분류한다")
  void resolve_externalApiWithBearer_returnsOauth2Credential() {
    MockHttpServletRequest request = externalApiRequest();
    request.addHeader("Authorization", "Bearer future-oauth-token");

    RequestCredential credential = resolver.resolve(request);

    assertThat(credential.type()).isEqualTo(AuthenticationType.OAUTH2_ACCESS_TOKEN);
  }

  @Test
  @DisplayName("API Key와 Authorization을 동시에 제출하면 거부한다")
  void resolve_multipleCredentialTypes_throwsAmbiguousCredentials() {
    MockHttpServletRequest request = externalApiRequest();
    request.addHeader(RequestCredentialResolver.API_KEY_HEADER, "test-api-key");
    request.addHeader("Authorization", "Bearer token");

    assertThatThrownBy(() -> resolver.resolve(request))
        .isInstanceOf(CredentialAuthenticationException.class)
        .hasMessage(AMBIGUOUS_CREDENTIALS.stringCode());
  }

  @Test
  @DisplayName("Authorization 헤더가 중복되면 거부한다")
  void resolve_duplicateAuthorizationHeaders_throwsAmbiguousCredentials() {
    MockHttpServletRequest request = request("GET", "/api/v1/auth/me");
    request.addHeader("Authorization", "Bearer first");
    request.addHeader("Authorization", "Bearer second");

    assertThatThrownBy(() -> resolver.resolve(request))
        .isInstanceOf(CredentialAuthenticationException.class)
        .hasMessage(AMBIGUOUS_CREDENTIALS.stringCode());
  }

  @Test
  @DisplayName("값이 없는 Bearer scheme은 거부한다")
  void resolve_emptyBearer_throwsLoginTokenInvalid() {
    MockHttpServletRequest request = request("GET", "/api/v1/auth/me");
    request.addHeader("Authorization", "Bearer ");

    assertThatThrownBy(() -> resolver.resolve(request))
        .isInstanceOf(CredentialAuthenticationException.class)
        .hasMessage(LOGIN_TOKEN_INVALID.stringCode());
  }

  @Test
  @DisplayName("대시보드의 잘못된 Authorization scheme은 거부한다")
  void resolve_dashboardWithWrongScheme_throwsLoginTokenInvalid() {
    MockHttpServletRequest request = request("GET", "/api/v1/auth/me");
    request.addHeader("Authorization", "Basic credentials");

    assertThatThrownBy(() -> resolver.resolve(request))
        .isInstanceOf(CredentialAuthenticationException.class)
        .hasMessage(LOGIN_TOKEN_INVALID.stringCode());
  }

  @Test
  @DisplayName("대시보드에 API Key를 제출하면 로그인 인증으로 대체하지 않는다")
  void resolve_dashboardWithApiKey_throwsLoginTokenInvalid() {
    MockHttpServletRequest request = request("GET", "/api/v1/dashboard/profile");
    request.addHeader(RequestCredentialResolver.API_KEY_HEADER, "test-api-key");

    assertThatThrownBy(() -> resolver.resolve(request))
        .isInstanceOf(CredentialAuthenticationException.class)
        .hasMessage(LOGIN_TOKEN_INVALID.stringCode());
  }

  @Test
  @DisplayName("외부 API에 인증 정보가 없으면 기존 UNAUTHORIZED 코드를 유지한다")
  void resolve_externalApiWithoutCredential_throwsLegacyUnauthorized() {
    MockHttpServletRequest request = externalApiRequest();

    assertThatThrownBy(() -> resolver.resolve(request))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage(UNAUTHORIZED.stringCode());
  }

  private MockHttpServletRequest externalApiRequest() {
    return request("POST", "/api/v1/filter");
  }

  private MockHttpServletRequest request(String method, String uri) {
    return new MockHttpServletRequest(method, uri);
  }
}
