package app.security.oauth2;

import app.application.auth.SsoLoginCompletionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
@EnableConfigurationProperties({SsoCookieProperties.class, SsoFrontendProperties.class})
public class OAuth2SecuritySupportConfig {
  static final String LOCAL_STUB_SIGNING_KEY = "stub-sso-cookie-signing-key-for-local-and-ci";

  @Bean
  SsoCookieSecurityConfigurationValidator ssoCookieSecurityConfigurationValidator(
      SsoCookieProperties properties, Environment environment) {
    return new SsoCookieSecurityConfigurationValidator(properties, environment);
  }

  @Bean
  CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository(
      SsoCookieProperties ssoCookieProperties) {
    return new CookieOAuth2AuthorizationRequestRepository(ssoCookieProperties);
  }

  @Bean
  OAuth2LoginSuccessHandler oauth2LoginSuccessHandler(
      SsoFrontendProperties ssoFrontendProperties,
      SsoLoginCompletionService loginCompletionService) {
    return new OAuth2LoginSuccessHandler(ssoFrontendProperties, loginCompletionService);
  }

  @Bean
  OAuth2LoginFailureHandler oauth2LoginFailureHandler(SsoFrontendProperties ssoFrontendProperties) {
    return new OAuth2LoginFailureHandler(ssoFrontendProperties);
  }

  static final class SsoCookieSecurityConfigurationValidator {
    SsoCookieSecurityConfigurationValidator(
        SsoCookieProperties properties, Environment environment) {
      if (environment.acceptsProfiles(Profiles.of("prod"))
          && LOCAL_STUB_SIGNING_KEY.equals(properties.signingKey())) {
        throw new IllegalStateException("SSO_COOKIE_SIGNING_KEY must be configured in prod");
      }
    }
  }
}
