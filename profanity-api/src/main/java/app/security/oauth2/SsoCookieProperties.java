package app.security.oauth2;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sso.cookie")
public record SsoCookieProperties(
    @NotBlank String name, @Min(1) long ttlSeconds, @NotBlank @Size(min = 32) String signingKey) {}
