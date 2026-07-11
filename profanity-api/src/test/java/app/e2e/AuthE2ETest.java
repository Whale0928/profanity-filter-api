package app.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.application.auth.LoginAuthService;
import app.core.data.response.constant.StatusCode;
import app.domain.user.OAuthLoginProfile;
import app.domain.user.OAuthProvider;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

class AuthE2ETest extends AbstractApiTester {
  private static final String REFRESH_COOKIE = "PF_LOGIN_REFRESH";

  @Autowired private MockMvc mockMvc;
  @Autowired private LoginAuthService loginAuthService;

  @Test
  @DisplayName("SSO 교환 코드로 access token과 HttpOnly refresh cookie를 발급한다")
  void exchange_whenSsoCodeIsValid_issuesLoginTokens() throws Exception {
    String code = issueExchangeCode("exchange-user");

    MockHttpServletResponse exchange = exchange(code);
    JsonNode body = body(exchange);

    assertThat(exchange.getStatus()).isEqualTo(200);
    assertThat(exchange.getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");
    assertThat(body.at("/status/code").asInt()).isEqualTo(StatusCode.OK.code());
    assertThat(body.at("/data/accessToken").asText()).isNotBlank();
    assertThat(body.at("/data/tokenType").asText()).isEqualTo("Bearer");
    assertThat(body.at("/data/expiresIn").asLong()).isPositive();
    assertThat(body.at("/data/user/id").asText()).isNotBlank();
    assertRefreshCookie(exchange.getCookie(REFRESH_COOKIE));

    MockHttpServletResponse me = me(body.at("/data/accessToken").asText());
    assertThat(me.getStatus()).isEqualTo(200);
    assertThat(body(me).at("/data/id").asText()).isEqualTo(body.at("/data/user/id").asText());
  }

  @Test
  @DisplayName("refresh token을 rotate하면 기존 token은 grace 안에서 실패하지만 family는 유지한다")
  void refresh_whenOldTokenIsReusedWithinGrace_keepsReplacementFamily() throws Exception {
    MockHttpServletResponse exchange = exchange(issueExchangeCode("rotation-user"));
    Cookie oldRefresh = exchange.getCookie(REFRESH_COOKIE);
    Csrf csrf = csrf(oldRefresh);

    MockHttpServletResponse rotated = refresh(oldRefresh, csrf);
    Cookie replacement = rotated.getCookie(REFRESH_COOKIE);
    assertThat(rotated.getStatus()).isEqualTo(200);
    assertRefreshCookie(replacement);
    assertThat(replacement.getValue()).isNotEqualTo(oldRefresh.getValue());

    MockHttpServletResponse duplicate = refresh(oldRefresh, csrf(oldRefresh));
    assertThat(duplicate.getStatus()).isEqualTo(401);
    assertThat(body(duplicate).at("/status/code").asInt())
        .isEqualTo(StatusCode.REFRESH_TOKEN_REUSED.code());
    assertThat(duplicate.getHeader(HttpHeaders.SET_COOKIE)).isNull();

    MockHttpServletResponse successorRotation = refresh(replacement, csrf(replacement));
    assertThat(successorRotation.getStatus()).isEqualTo(200);
    assertThat(body(successorRotation).at("/data/accessToken").asText()).isNotBlank();
  }

  @Test
  @DisplayName("refresh 요청에 CSRF token이 없으면 403을 반환한다")
  void refresh_whenCsrfIsMissing_returnsForbidden() throws Exception {
    Cookie refreshCookie = exchange(issueExchangeCode("csrf-user")).getCookie(REFRESH_COOKIE);

    MockHttpServletResponse response =
        mockMvc
            .perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(body(response).at("/status/code").asInt()).isEqualTo(StatusCode.FORBIDDEN.code());
  }

  @Test
  @DisplayName("이미 소비한 로그인 교환 코드는 다시 사용할 수 없다")
  void exchange_whenCodeIsReused_returnsUnauthorized() throws Exception {
    String code = issueExchangeCode("reused-code-user");
    assertThat(exchange(code).getStatus()).isEqualTo(200);

    MockHttpServletResponse reused = exchange(code);

    assertThat(reused.getStatus()).isEqualTo(401);
    assertThat(body(reused).at("/status/code").asInt())
        .isEqualTo(StatusCode.LOGIN_CODE_INVALID.code());
  }

  @Test
  @DisplayName("외부 API의 Bearer token은 OAuth2 미지원 오류로 fail-closed 처리한다")
  void externalApi_whenBearerIsSubmitted_returnsOAuth2Unsupported() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/api/v1/filter")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer unsupported-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"hello\",\"mode\":\"QUICK\"}"))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(body(response).at("/status/code").asInt())
        .isEqualTo(StatusCode.OAUTH2_ACCESS_TOKEN_UNSUPPORTED.code());
  }

  @Test
  @DisplayName("API Key와 Authorization을 함께 보내면 요청을 거부한다")
  void externalApi_whenMultipleCredentialsAreSubmitted_returnsBadRequest() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/api/v1/filter")
                    .header("X-API-KEY", "one-key")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer other-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"hello\",\"mode\":\"QUICK\"}"))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(body(response).at("/status/code").asInt())
        .isEqualTo(StatusCode.AMBIGUOUS_CREDENTIALS.code());
  }

  @Test
  @DisplayName("로그인 교환 endpoint도 다중 credential을 거부하고 코드를 소비하지 않는다")
  void exchange_whenMultipleCredentialsAreSubmitted_rejectsBeforeCodeConsumption()
      throws Exception {
    String code = issueExchangeCode("exchange-conflict-user");
    MockHttpServletResponse conflict =
        mockMvc
            .perform(
                post("/api/v1/auth/exchange")
                    .header("X-API-KEY", "api-key")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer login-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("code", code))))
            .andReturn()
            .getResponse();

    assertThat(conflict.getStatus()).isEqualTo(400);
    assertThat(body(conflict).at("/status/code").asInt())
        .isEqualTo(StatusCode.AMBIGUOUS_CREDENTIALS.code());
    assertThat(exchange(code).getStatus()).isEqualTo(200);
  }

  @Test
  @DisplayName("대시보드 사용자 endpoint는 API Key 인증을 허용하지 않는다")
  void me_whenApiKeyIsSubmitted_returnsLoginTokenInvalid() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(get("/api/v1/auth/me").header("X-API-KEY", "legacy-api-key"))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(body(response).at("/status/code").asInt())
        .isEqualTo(StatusCode.LOGIN_TOKEN_INVALID.code());
  }

  @Test
  @DisplayName("허용된 frontend origin의 credentialed preflight를 허용한다")
  void cors_whenAllowedFrontendRequestsPreflight_allowsCredentials() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(
                options("/api/v1/auth/refresh")
                    .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-XSRF-TOKEN"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
        .isEqualTo("http://localhost:5173");
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
  }

  @Test
  @DisplayName("허용되지 않은 frontend origin의 로그인 preflight를 거부한다")
  void cors_whenFrontendOriginIsNotAllowed_rejectsCredentialedPreflight() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(
                options("/api/v1/auth/refresh")
                    .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-XSRF-TOKEN"))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isNull();
  }

  @Test
  @DisplayName("기존 외부 API의 wildcard CORS 계약을 유지한다")
  void cors_whenExternalApiRequestsPreflight_keepsLegacyWildcardPolicy() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(
                options("/api/v1/filter")
                    .header(HttpHeaders.ORIGIN, "https://client.example")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-API-KEY"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isNull();
  }

  @Test
  @DisplayName("대시보드 endpoint도 로그인 frontend origin에만 credential을 허용한다")
  void cors_whenDashboardRequestsPreflight_usesLoginOriginPolicy() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(
                options("/api/v1/dashboard/profile")
                    .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
        .isEqualTo("http://localhost:5173");
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
  }

  private String issueExchangeCode(String providerUserId) {
    return loginAuthService.issueExchangeCode(
        new OAuthLoginProfile(
            OAuthProvider.GOOGLE,
            providerUserId,
            providerUserId + "@gmail.com",
            true,
            true,
            providerUserId + "@gmail.com",
            providerUserId,
            null));
  }

  private MockHttpServletResponse exchange(String code) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/auth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("code", code))))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse me(String accessToken) throws Exception {
    return mockMvc
        .perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andReturn()
        .getResponse();
  }

  private Csrf csrf(Cookie refreshCookie) throws Exception {
    MockHttpServletResponse response =
        mockMvc.perform(get("/api/v1/auth/csrf").cookie(refreshCookie)).andReturn().getResponse();
    JsonNode body = body(response);
    return new Csrf(
        body.at("/data/headerName").asText(),
        body.at("/data/token").asText(),
        response.getCookie("XSRF-TOKEN"));
  }

  private MockHttpServletResponse refresh(Cookie refreshCookie, Csrf csrf) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .cookie(refreshCookie, csrf.cookie())
                .header(csrf.headerName(), csrf.token()))
        .andReturn()
        .getResponse();
  }

  private JsonNode body(MockHttpServletResponse response) throws Exception {
    return objectMapper.readTree(response.getContentAsString());
  }

  private void assertRefreshCookie(Cookie cookie) {
    assertThat(cookie).isNotNull();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getSecure()).isFalse();
    assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
    assertThat(cookie.getMaxAge()).isPositive();
    assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
    assertThat(cookie.getDomain()).isNull();
  }

  private record Csrf(String headerName, String token, Cookie cookie) {}
}
