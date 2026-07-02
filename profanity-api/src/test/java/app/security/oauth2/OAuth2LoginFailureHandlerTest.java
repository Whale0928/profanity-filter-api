package app.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import app.core.data.response.constant.StatusCode;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class OAuth2LoginFailureHandlerTest {

  @Test
  @DisplayName("OAuth2 로그인 실패 시 표준 status 코드와 상세 사유를 FE fragment로 redirect한다")
  void onAuthenticationFailure_whenOAuth2LoginFailed_redirectsToFrontendFragment()
      throws Exception {
    OAuth2LoginFailureHandler failureHandler = new OAuth2LoginFailureHandler();
    MockHttpServletResponse response = new MockHttpServletResponse();

    failureHandler.onAuthenticationFailure(
        new MockHttpServletRequest(), response, new BadCredentialsException("state mismatch"));

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
    assertThat(response.getRedirectedUrl())
        .startsWith("http://localhost:63344/profanity-filter-api/sso/index.html#")
        .contains("error=oauth2_login_failed")
        .contains("statusCode=" + StatusCode.OAUTH2_LOGIN_FAILED.code())
        .contains("statusMessage=" + StatusCode.OAUTH2_LOGIN_FAILED.status())
        .contains("statusDetailDescription=state%20mismatch");
  }
}
