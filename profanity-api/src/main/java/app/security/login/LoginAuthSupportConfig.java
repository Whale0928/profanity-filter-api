package app.security.login;

import java.net.URI;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
@EnableConfigurationProperties(LoginSessionProperties.class)
public class LoginAuthSupportConfig {

  @Bean
  Clock loginAuthClock() {
    return Clock.systemUTC();
  }

  @Bean
  LoginSecurityConfigurationValidator loginSecurityConfigurationValidator(
      LoginSessionProperties properties, Environment environment) {
    return new LoginSecurityConfigurationValidator(properties, environment);
  }

  static final class LoginSecurityConfigurationValidator {
    LoginSecurityConfigurationValidator(
        LoginSessionProperties properties, Environment environment) {
      if (properties.allowedOrigins().stream().anyMatch("*"::equals)) {
        throw new IllegalStateException("Credentialed login CORS cannot use a wildcard origin");
      }
      properties.allowedOrigins().forEach(LoginSecurityConfigurationValidator::validateOrigin);
      if (environment.acceptsProfiles(Profiles.of("prod"))
          && !properties.refreshCookie().secure()) {
        throw new IllegalStateException("LOGIN_REFRESH_COOKIE_SECURE must be true in prod");
      }
    }

    private static void validateOrigin(String origin) {
      URI uri;
      try {
        uri = URI.create(origin);
      } catch (IllegalArgumentException exception) {
        throw new IllegalStateException("Invalid SSO frontend origin", exception);
      }
      if (uri.getScheme() == null
          || uri.getHost() == null
          || uri.getPath() != null && !uri.getPath().isEmpty()
          || uri.getQuery() != null
          || uri.getFragment() != null) {
        throw new IllegalStateException("SSO frontend origins must contain scheme and host only");
      }
    }
  }
}
