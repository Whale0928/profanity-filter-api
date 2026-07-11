package app.security.oauth2;

import app.core.data.response.constant.StatusCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

  private final SsoFrontendProperties ssoFrontendProperties;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    log.warn("OAuth2 login failed. exceptionType={}", exception.getClass().getSimpleName());

    response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
    response.setHeader(HttpHeaders.PRAGMA, "no-cache");
    response.sendRedirect(
        ssoFrontendProperties.redirectUri()
            + "#error=oauth2_login_failed"
            + "&statusCode="
            + StatusCode.OAUTH2_LOGIN_FAILED.code());
  }
}
