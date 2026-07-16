package app.security.authentication;

import static app.core.data.response.constant.StatusCode.LOGIN_TOKEN_INVALID;
import static app.core.data.response.constant.StatusCode.OAUTH2_ACCESS_TOKEN_UNSUPPORTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.security.filter.AuthenticationRoutePolicy;
import app.security.filter.RequestCredential;
import app.security.filter.RequestCredentialResolver;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;

class AuthenticationServiceTest {

  @Test
  @DisplayName("대시보드 JWT 검증 실패 후 API Key 인증기로 fallback하지 않는다")
  void getAuthentication_loginJwtFailure_doesNotFallbackToApiKey() {
    CountingAuthenticator loginAuthenticator =
        new CountingAuthenticator(AuthenticationType.LOGIN_JWT, true);
    CountingAuthenticator apiKeyAuthenticator =
        new CountingAuthenticator(AuthenticationType.API_KEY, false);
    AuthenticationService service = service(loginAuthenticator, apiKeyAuthenticator);
    MockHttpServletRequest request = request("GET", "/api/v1/auth/me");
    request.addHeader("Authorization", "Bearer invalid-login-token");

    assertThatThrownBy(() -> service.getAuthentication(request))
        .isInstanceOf(CredentialAuthenticationException.class)
        .hasMessage(LOGIN_TOKEN_INVALID.stringCode());
    assertThat(loginAuthenticator.invocationCount).isEqualTo(1);
    assertThat(apiKeyAuthenticator.invocationCount).isZero();
  }

  @Test
  @DisplayName("외부 API Bearer token은 OAuth2 미지원 오류로 fail-closed 처리한다")
  void getAuthentication_externalBearer_throwsOauth2Unsupported() {
    CountingAuthenticator loginAuthenticator =
        new CountingAuthenticator(AuthenticationType.LOGIN_JWT, false);
    CountingAuthenticator apiKeyAuthenticator =
        new CountingAuthenticator(AuthenticationType.API_KEY, false);
    AuthenticationService service = service(loginAuthenticator, apiKeyAuthenticator);
    MockHttpServletRequest request = request("POST", "/api/v1/filter");
    request.addHeader("Authorization", "Bearer future-token");

    assertThatThrownBy(() -> service.getAuthentication(request))
        .isInstanceOf(CredentialAuthenticationException.class)
        .hasMessage(OAUTH2_ACCESS_TOKEN_UNSUPPORTED.stringCode());
    assertThat(loginAuthenticator.invocationCount).isZero();
    assertThat(apiKeyAuthenticator.invocationCount).isZero();
  }

  @Test
  @DisplayName("동일 인증 타입의 인증기가 둘이면 애플리케이션 구성을 거부한다")
  void constructor_duplicateAuthenticatorType_throwsIllegalStateException() {
    RequestCredentialResolver resolver =
        new RequestCredentialResolver(new AuthenticationRoutePolicy());

    assertThatThrownBy(
            () ->
                new AuthenticationService(
                    resolver,
                    List.of(
                        new CountingAuthenticator(AuthenticationType.API_KEY, false),
                        new CountingAuthenticator(AuthenticationType.API_KEY, false))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(AuthenticationType.API_KEY.name());
  }

  private AuthenticationService service(RequestAuthenticator... authenticators) {
    return new AuthenticationService(
        new RequestCredentialResolver(new AuthenticationRoutePolicy()), List.of(authenticators));
  }

  private MockHttpServletRequest request(String method, String uri) {
    return new MockHttpServletRequest(method, uri);
  }

  private static final class CountingAuthenticator implements RequestAuthenticator {
    private final AuthenticationType type;
    private final boolean fail;
    private int invocationCount;

    private CountingAuthenticator(AuthenticationType type, boolean fail) {
      this.type = type;
      this.fail = fail;
    }

    @Override
    public AuthenticationType supports() {
      return type;
    }

    @Override
    public Authentication authenticate(RequestCredential credential) {
      invocationCount++;
      if (fail) {
        throw new CredentialAuthenticationException(HttpStatus.UNAUTHORIZED, LOGIN_TOKEN_INVALID);
      }
      return new CustomAuthentication(
          type,
          null,
          List.of(),
          type == AuthenticationType.LOGIN_JWT
              ? new LoginUserPrincipal(UUID.randomUUID(), "user@example.com")
              : new ApiKeyPrincipal(
                  UUID.randomUUID(),
                  "client@example.com",
                  "test",
                  List.of("READ"),
                  "now",
                  "key-hash"));
    }
  }
}
