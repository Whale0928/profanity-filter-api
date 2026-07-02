package app.security.oauth2;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SsoCookieProperties.class)
public class OAuth2SecuritySupportConfig {

  @Bean
  CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository(
      SsoCookieProperties ssoCookieProperties) {
    return new CookieOAuth2AuthorizationRequestRepository(ssoCookieProperties);
  }

  @Bean
  OAuth2LoginSuccessHandler oauth2LoginSuccessHandler() {
    return new OAuth2LoginSuccessHandler();
  }

  @Bean
  OAuth2LoginFailureHandler oauth2LoginFailureHandler() {
    return new OAuth2LoginFailureHandler();
  }
}
