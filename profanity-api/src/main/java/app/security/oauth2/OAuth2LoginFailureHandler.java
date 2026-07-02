package app.security.oauth2;

import app.core.data.response.Status;
import app.core.data.response.constant.StatusCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@Slf4j
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

  private static final String FRONTEND_REDIRECT_URI =
      "http://localhost:63344/profanity-filter-api/sso/index.html";

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    Status status = Status.of(StatusCode.OAUTH2_LOGIN_FAILED, exception.getMessage());

    log.warn("OAuth2 login failed. message={}", exception.getMessage());

    response.sendRedirect(
        FRONTEND_REDIRECT_URI
            + "#error=oauth2_login_failed"
            + "&statusCode="
            + status.code()
            + "&statusMessage="
            + encode(status.message())
            + "&statusDescription="
            + encode(status.description())
            + "&statusDetailDescription="
            + encode(status.DetailDescription()));
  }

  private String encode(String value) {
    if (value == null) {
      return "";
    }
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
