package app.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtSecurityProperties(
    @NotBlank String issuer,
    @NotBlank String audience,
    @NotNull Duration accessTokenTtl,
    @NotNull Duration clockSkew) {

  public JwtSecurityProperties {
    if (accessTokenTtl != null && (accessTokenTtl.isZero() || accessTokenTtl.isNegative())) {
      throw new IllegalArgumentException("accessTokenTtl must be positive");
    }
    if (clockSkew != null && clockSkew.isNegative()) {
      throw new IllegalArgumentException("clockSkew must not be negative");
    }
  }
}
