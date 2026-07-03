package app.security.oauth2;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SsoCookieProperties.class, SsoFrontendProperties.class})
public class OAuth2SecuritySupportConfig {

  @Bean
  CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository(
      SsoCookieProperties ssoCookieProperties) {
    return new CookieOAuth2AuthorizationRequestRepository(ssoCookieProperties);
  }

  @Bean
  OAuth2LoginSuccessHandler oauth2LoginSuccessHandler(SsoFrontendProperties ssoFrontendProperties) {
    return new OAuth2LoginSuccessHandler(ssoFrontendProperties);
  }

  @Bean
  OAuth2LoginFailureHandler oauth2LoginFailureHandler(SsoFrontendProperties ssoFrontendProperties) {
    return new OAuth2LoginFailureHandler(ssoFrontendProperties);
  }
}
