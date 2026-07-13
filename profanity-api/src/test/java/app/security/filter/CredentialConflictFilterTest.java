package app.security.filter;

import static org.assertj.core.api.Assertions.assertThat;

import app.core.data.response.constant.StatusCode;
import jakarta.servlet.DispatcherType;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

class CredentialConflictFilterTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("public auth endpoint에서도 다중 credential을 거부하고 요청 처리를 중단한다")
  void doFilter_publicAuthWithMultipleCredentials_rejectsConflict() throws Exception {
    RequestCredentialResolver resolver =
        new RequestCredentialResolver(new AuthenticationRoutePolicy());
    CustomAuthenticationEntryPoint entryPoint =
        new CustomAuthenticationEntryPoint(
            (request, response, handler, exception) -> new ModelAndView());
    CredentialConflictFilter filter = new CredentialConflictFilter(resolver, entryPoint);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/exchange");
    request.addHeader(RequestCredentialResolver.API_KEY_HEADER, "api-key");
    request.addHeader("Authorization", "Bearer login-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean filterChainCalled = new AtomicBoolean();

    filter.doFilter(
        request, response, (chainRequest, chainResponse) -> filterChainCalled.set(true));

    assertThat(filterChainCalled).isFalse();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(request.getAttribute("exception").toString())
        .contains(StatusCode.AMBIGUOUS_CREDENTIALS.stringCode());
  }

  @Test
  @DisplayName("단일 또는 없는 credential은 public auth endpoint로 전달한다")
  void doFilter_publicAuthWithoutConflict_continuesChain() throws Exception {
    CredentialConflictFilter filter =
        new CredentialConflictFilter(
            new RequestCredentialResolver(new AuthenticationRoutePolicy()), null);
    List<MockHttpServletRequest> requests =
        List.of(
            new MockHttpServletRequest("POST", "/api/v1/auth/exchange"),
            requestWithHeader("X-API-KEY", "api-key"),
            requestWithHeader("Authorization", "Bearer login-token"));

    for (MockHttpServletRequest request : requests) {
      AtomicBoolean filterChainCalled = new AtomicBoolean();
      filter.doFilter(
          request,
          new MockHttpServletResponse(),
          (chainRequest, chainResponse) -> filterChainCalled.set(true));
      assertThat(filterChainCalled).isTrue();
    }
  }

  @Test
  @DisplayName("preflight와 ERROR dispatch는 credential 충돌 검사에서 제외한다")
  void shouldNotFilter_preflightAndErrorDispatch_returnsTrue() {
    CredentialConflictFilter filter = new CredentialConflictFilter(null, null);
    MockHttpServletRequest preflight =
        new MockHttpServletRequest("OPTIONS", "/api/v1/auth/refresh");
    MockHttpServletRequest error = new MockHttpServletRequest("GET", "/error");
    error.setDispatcherType(DispatcherType.ERROR);

    assertThat(filter.shouldNotFilter(preflight)).isTrue();
    assertThat(filter.shouldNotFilter(error)).isTrue();
  }

  private MockHttpServletRequest requestWithHeader(String name, String value) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/exchange");
    request.addHeader(name, value);
    return request;
  }
}
