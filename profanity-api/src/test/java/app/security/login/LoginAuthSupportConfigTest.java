package app.security.login;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class LoginAuthSupportConfigTest {

  @Test
  @DisplayName("운영 환경의 refresh cookie가 Secure가 아니면 시작을 거부한다")
  void validator_whenProductionCookieIsNotSecure_throwsIllegalStateException() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");

    assertThatThrownBy(
            () ->
                new LoginAuthSupportConfig.LoginSecurityConfigurationValidator(
                    properties(List.of("https://dashboard.example"), false), environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("LOGIN_REFRESH_COOKIE_SECURE");
  }

  @Test
  @DisplayName("credentialed 로그인 CORS에 wildcard origin을 허용하지 않는다")
  void validator_whenAllowedOriginIsWildcard_throwsIllegalStateException() {
    assertThatThrownBy(
            () ->
                new LoginAuthSupportConfig.LoginSecurityConfigurationValidator(
                    properties(List.of("*"), true), new MockEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("wildcard");
  }

  @Test
  @DisplayName("로그인 CORS origin에 path가 포함되면 시작을 거부한다")
  void validator_whenAllowedOriginContainsPath_throwsIllegalStateException() {
    assertThatThrownBy(
            () ->
                new LoginAuthSupportConfig.LoginSecurityConfigurationValidator(
                    properties(List.of("https://dashboard.example/login"), true),
                    new MockEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("scheme and host only");
  }

  private LoginSessionProperties properties(List<String> origins, boolean secure) {
    return new LoginSessionProperties(
        Duration.ofMinutes(1),
        Duration.ofDays(14),
        Duration.ofDays(30),
        Duration.ofSeconds(5),
        origins,
        new LoginSessionProperties.RefreshCookie(
            "PF_LOGIN_REFRESH", secure, "Strict", "/api/v1/auth"));
  }
}
