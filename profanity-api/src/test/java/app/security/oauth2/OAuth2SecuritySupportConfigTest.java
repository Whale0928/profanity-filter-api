package app.security.oauth2;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class OAuth2SecuritySupportConfigTest {

  @Test
  @DisplayName("운영 환경에서 공개된 local signing key 기본값을 거부한다")
  void validator_whenProductionUsesLocalStubSigningKey_throwsIllegalStateException() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    SsoCookieProperties properties =
        new SsoCookieProperties(
            "PF_OAUTH2_AUTHORIZATION_REQUEST",
            300,
            OAuth2SecuritySupportConfig.LOCAL_STUB_SIGNING_KEY);

    assertThatThrownBy(
            () ->
                new OAuth2SecuritySupportConfig.SsoCookieSecurityConfigurationValidator(
                    properties, environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SSO_COOKIE_SIGNING_KEY");
  }

  @Test
  @DisplayName("운영 환경에서 별도 signing key를 설정하면 구성을 허용한다")
  void validator_whenProductionUsesConfiguredSigningKey_acceptsConfiguration() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    SsoCookieProperties properties =
        new SsoCookieProperties("PF_OAUTH2_AUTHORIZATION_REQUEST", 300, "x".repeat(32));

    assertThatCode(
            () ->
                new OAuth2SecuritySupportConfig.SsoCookieSecurityConfigurationValidator(
                    properties, environment))
        .doesNotThrowAnyException();
  }
}
