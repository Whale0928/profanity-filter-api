package app.security.login;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginRefreshCookieWriter {
  private final LoginSessionProperties properties;

  public void write(HttpServletResponse response, String refreshToken, Duration maxAge) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookie(refreshToken, maxAge).toString());
  }

  public void expire(HttpServletResponse response) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
  }

  public String cookieName() {
    return properties.refreshCookie().name();
  }

  private ResponseCookie cookie(String value, Duration maxAge) {
    LoginSessionProperties.RefreshCookie properties = this.properties.refreshCookie();
    return ResponseCookie.from(properties.name(), value)
        .httpOnly(true)
        .secure(properties.secure())
        .sameSite(properties.sameSite())
        .path(properties.path())
        .maxAge(maxAge)
        .build();
  }
}
