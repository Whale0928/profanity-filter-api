package app.security.filter;

import static app.core.data.response.constant.StatusCode.LOGIN_TOKEN_INVALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.security.authentication.AuthenticationService;
import app.security.authentication.AuthenticationType;
import app.security.authentication.CredentialAuthenticationException;
import app.security.authentication.CustomAuthentication;
import app.security.authentication.LoginUserPrincipal;
import app.security.authentication.RequestAuthenticator;
import jakarta.servlet.DispatcherType;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

class CustomAuthenticationFilterTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("인증 실패 시 SecurityContext를 비우고 다음 filter를 호출하지 않는다")
  void doFilter_authenticationFails_clearsContextAndStopsChain() throws Exception {
    RequestAuthenticator failingAuthenticator =
        new RequestAuthenticator() {
          @Override
          public AuthenticationType supports() {
            return AuthenticationType.LOGIN_JWT;
          }

          @Override
          public org.springframework.security.core.Authentication authenticate(
              RequestCredential credential) {
            throw new CredentialAuthenticationException(
                HttpStatus.UNAUTHORIZED, LOGIN_TOKEN_INVALID);
          }
        };
    RequestCredentialResolver credentialResolver =
        new RequestCredentialResolver(new AuthenticationRoutePolicy());
    AuthenticationService authenticationService =
        new AuthenticationService(credentialResolver, List.of(failingAuthenticator));
    CustomAuthenticationEntryPoint entryPoint =
        new CustomAuthenticationEntryPoint(
            (request, response, handler, exception) -> new ModelAndView());
    CustomAuthenticationFilter filter =
        new CustomAuthenticationFilter(authenticationService, entryPoint);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
    request.addHeader("Authorization", "Bearer invalid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean filterChainCalled = new AtomicBoolean();

    filter.doFilter(
        request, response, (chainRequest, chainResponse) -> filterChainCalled.set(true));

    assertThat(filterChainCalled).isFalse();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  @DisplayName("정상 LOGIN_JWT 인증은 빈 SecurityContext에 정확히 한 인증 객체를 설정한다")
  void doFilter_validLoginJwt_setsSingleAuthenticationAndContinuesChain() throws Exception {
    UUID userId = UUID.randomUUID();
    RequestAuthenticator loginAuthenticator =
        new RequestAuthenticator() {
          @Override
          public AuthenticationType supports() {
            return AuthenticationType.LOGIN_JWT;
          }

          @Override
          public org.springframework.security.core.Authentication authenticate(
              RequestCredential credential) {
            return new CustomAuthentication(
                AuthenticationType.LOGIN_JWT,
                null,
                List.of(new SimpleGrantedAuthority("AUTH_LOGIN_JWT")),
                new LoginUserPrincipal(userId, "user@example.com"));
          }
        };
    AuthenticationService authenticationService =
        new AuthenticationService(
            new RequestCredentialResolver(new AuthenticationRoutePolicy()),
            List.of(loginAuthenticator));
    CustomAuthenticationFilter filter = new CustomAuthenticationFilter(authenticationService, null);
    MockHttpServletRequest request = request("GET", "/api/v1/auth/me");
    request.addHeader("Authorization", "Bearer valid-token");
    AtomicReference<org.springframework.security.core.Authentication> observed =
        new AtomicReference<>();

    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        (chainRequest, chainResponse) ->
            observed.set(SecurityContextHolder.getContext().getAuthentication()));

    assertThat(observed.get()).isInstanceOf(CustomAuthentication.class);
    assertThat(((CustomAuthentication) observed.get()).authenticationType())
        .isEqualTo(AuthenticationType.LOGIN_JWT);
    assertThat(((LoginUserPrincipal) observed.get().getPrincipal()).id()).isEqualTo(userId);
  }

  @Test
  @DisplayName("로그인 교환과 갱신 endpoint만 API Key filter에서 정확히 제외한다")
  void shouldNotFilter_publicAuthEndpoints_excludesOnlyExactRoutes() {
    CustomAuthenticationFilter filter = new CustomAuthenticationFilter(null, null);

    assertThat(filter.shouldNotFilter(request("POST", "/api/v1/auth/exchange"))).isTrue();
    assertThat(filter.shouldNotFilter(request("GET", "/api/v1/auth/csrf"))).isTrue();
    assertThat(filter.shouldNotFilter(request("POST", "/api/v1/auth/refresh"))).isTrue();
    assertThat(filter.shouldNotFilter(request("GET", "/api/v1/auth/me"))).isFalse();
    assertThat(filter.shouldNotFilter(request("POST", "/api/v1/auth/exchange-extra"))).isFalse();
    assertThat(filter.shouldNotFilter(request("GET", "/api/v1/auth/refresh"))).isFalse();
  }

  @Test
  @DisplayName("CORS preflight와 ERROR dispatch는 인증 filter에서 제외한다")
  void shouldNotFilter_preflightAndErrorDispatch_returnsTrue() {
    CustomAuthenticationFilter filter = new CustomAuthenticationFilter(null, null);
    MockHttpServletRequest preflight = request("OPTIONS", "/api/v1/auth/me");
    MockHttpServletRequest error = request("GET", "/error");
    error.setDispatcherType(DispatcherType.ERROR);

    assertThat(filter.shouldNotFilter(preflight)).isTrue();
    assertThat(filter.shouldNotFilter(error)).isTrue();
  }

  @Test
  @DisplayName("예상하지 못한 일반 예외는 인증 오류로 숨기지 않고 전파한다")
  void doFilter_unexpectedException_propagatesException() {
    RequestAuthenticator brokenAuthenticator =
        new RequestAuthenticator() {
          @Override
          public AuthenticationType supports() {
            return AuthenticationType.LOGIN_JWT;
          }

          @Override
          public org.springframework.security.core.Authentication authenticate(
              RequestCredential credential) {
            throw new IllegalStateException("unexpected failure");
          }
        };
    AuthenticationService authenticationService =
        new AuthenticationService(
            new RequestCredentialResolver(new AuthenticationRoutePolicy()),
            List.of(brokenAuthenticator));
    CustomAuthenticationFilter filter = new CustomAuthenticationFilter(authenticationService, null);
    MockHttpServletRequest request = request("GET", "/api/v1/auth/me");
    request.addHeader("Authorization", "Bearer token");

    assertThatThrownBy(
            () ->
                filter.doFilter(
                    request, new MockHttpServletResponse(), (chainRequest, chainResponse) -> {}))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("unexpected failure");
  }

  private MockHttpServletRequest request(String method, String uri) {
    return new MockHttpServletRequest(method, uri);
  }
}
