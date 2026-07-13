package app.security.login;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.login")
public record LoginSessionProperties(
    @NotNull Duration exchangeCodeTtl,
    @NotNull Duration refreshTokenTtl,
    @NotNull Duration absoluteSessionTtl,
    @NotNull Duration refreshReuseGrace,
    @NotEmpty List<String> allowedOrigins,
    @Valid @NotNull RefreshCookie refreshCookie) {

  public record RefreshCookie(
      @NotBlank String name, boolean secure, @NotBlank String sameSite, @NotBlank String path) {}
}
