package app.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.TestConfig;
import app.application.auth.SsoLoginCompletionService;
import app.application.client.APIKeyGenerator;
import app.core.data.constant.Mode;
import app.core.data.response.constant.StatusCode;
import app.dto.request.ApiRequest;
import app.security.SecurityConfig;
import app.security.authentication.ApiKeyAuthenticator;
import app.security.authentication.AuthenticationService;
import app.security.filter.AuthenticationRoutePolicy;
import app.security.filter.CustomAccessDeniedHandler;
import app.security.filter.CustomAuthenticationEntryPoint;
import app.security.filter.CustomAuthenticationFilter;
import app.security.filter.RequestCredentialResolver;
import app.security.login.LoginSessionProperties;
import app.test.support.config.SecurityFakeStubConfig;
import app.test.support.fake.FakeApiKeyMetadataReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProfanityController.class)
@ExtendWith(OutputCaptureExtension.class)
@Import({
  TestConfig.class,
  SecurityFakeStubConfig.class,
  SecurityConfig.class,
  CustomAuthenticationFilter.class,
  CustomAuthenticationEntryPoint.class,
  CustomAccessDeniedHandler.class,
  AuthenticationService.class,
  ApiKeyAuthenticator.class,
  AuthenticationRoutePolicy.class,
  RequestCredentialResolver.class,
  APIKeyGenerator.class,
  SecurityAuthenticationTest.SecurityTestConfig.class
})
class SecurityAuthenticationTest {
  private static final String REQUEST_URL = "/api/v1/filter";
  private String validApiKey;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("유효한 API 키 요청시 200 OK와 함께 성공 응답을 반환한다")
  void test_200() throws Exception {
    validApiKey = FakeApiKeyMetadataReader.validKeys.get(0);
    ApiRequest request = quickRequest("test text");
    mockMvc
        .perform(
            post(REQUEST_URL)
                .header("X-API-KEY", validApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(jsonPath("$.status.code").value(2000))
        .andExpect(jsonPath("$.status.message").value(StatusCode.OK.status()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("대시보드에 로그인 JWT가 없으면 HTTP 401과 LOGIN_TOKEN_INVALID를 반환한다")
  void authMe_missingLoginJwt_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status.code").value(StatusCode.LOGIN_TOKEN_INVALID.code()));
  }

  @Test
  @DisplayName("외부 API의 Bearer token은 HTTP 401과 OAuth2 미지원 코드를 반환한다")
  void filter_bearerToken_returnsOauth2Unsupported() throws Exception {
    mockMvc
        .perform(
            post(REQUEST_URL)
                .header("Authorization", "Bearer future-oauth-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(quickRequest("test text"))))
        .andExpect(status().isUnauthorized())
        .andExpect(
            jsonPath("$.status.code").value(StatusCode.OAUTH2_ACCESS_TOKEN_UNSUPPORTED.code()));
  }

  @Test
  @DisplayName("API Key와 Authorization을 함께 제출하면 HTTP 400으로 거부한다")
  void filter_multipleCredentials_returnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post(REQUEST_URL)
                .header("X-API-KEY", "test-api-key")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(quickRequest("test text"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status.code").value(StatusCode.AMBIGUOUS_CREDENTIALS.code()));
  }

  @Test
  @DisplayName("API 키가 비어있는 경우 4010 UNAUTHORIZED 응답을 반환한다")
  void test_4010(CapturedOutput output) throws Exception {
    ApiRequest request = quickRequest("test text");
    mockMvc
        .perform(
            post(REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-KEY", "")
                .header("CF-Connecting-IP", "203.0.113.8")
                .header("CF-Ray", "abc123-ICN")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status.code").value(4010))
        .andExpect(jsonPath("$.status.message").value(StatusCode.UNAUTHORIZED.status()));

    assertThat(output.getOut())
        .contains(
            "[AUTH] 인증 거절",
            "statusCode=4010",
            "httpStatus=200",
            "method=POST",
            "path=/api/v1/filter",
            "host=localhost",
            "clientIp=203.0.113.8",
            "credential=API_KEY_BLANK",
            "cfRay=abc123-ICN",
            "exceptionType=BadCredentialsException");
  }

  @Test
  @DisplayName("잘못된 형식의 API 키 요청시 4031 INVALID_API_KEY 응답을 반환한다")
  void test_4031(CapturedOutput output) throws Exception {
    ApiRequest request = quickRequest("test text");

    mockMvc
        .perform(
            post(REQUEST_URL)
                .header("X-API-KEY", "invalid-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status.code").value(StatusCode.INVALID_API_KEY.code()))
        .andExpect(jsonPath("$.status.message").value(StatusCode.INVALID_API_KEY.status()));

    assertThat(output.getOut())
        .contains("credential=API_KEY_PRESENT")
        .doesNotContain("invalid-api-key");
  }

  @Test
  @DisplayName("존재하지 않는 API 키 요청시 4040 NOT_FOUND_CLIENT 응답을 반환한다")
  void test_4040() throws Exception {
    ApiRequest request = quickRequest("test text");
    String key = FakeApiKeyMetadataReader.validKeys.get(0);
    FakeApiKeyMetadataReader.validKeys.remove(key);

    mockMvc
        .perform(
            post(REQUEST_URL)
                .header("X-API-KEY", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status.code").value(StatusCode.NOT_FOUND_CLIENT.code()))
        .andExpect(jsonPath("$.status.message").value(StatusCode.NOT_FOUND_CLIENT.status()));

    FakeApiKeyMetadataReader.validKeys.add(key);
  }

  private static ApiRequest quickRequest(String text) {
    return new ApiRequest(text, Mode.QUICK, null);
  }

  @TestConfiguration
  static class SecurityTestConfig {
    @Bean
    SsoLoginCompletionService ssoLoginCompletionService() {
      return profile -> "unused-test-code";
    }

    @Bean
    LoginSessionProperties loginSessionProperties() {
      return new LoginSessionProperties(
          Duration.ofMinutes(1),
          Duration.ofDays(14),
          Duration.ofDays(30),
          Duration.ofSeconds(3),
          List.of("http://localhost:5173"),
          new LoginSessionProperties.RefreshCookie("refresh_token", false, "Lax", "/"));
    }
  }
}
