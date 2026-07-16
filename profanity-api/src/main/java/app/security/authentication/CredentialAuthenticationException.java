package app.security.authentication;

import app.core.data.response.constant.StatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;

/** 인증 실패의 API 응답 코드와 HTTP 상태를 함께 전달합니다. */
public class CredentialAuthenticationException extends AuthenticationException {
  private final HttpStatus httpStatus;

  public CredentialAuthenticationException(HttpStatus httpStatus, StatusCode statusCode) {
    super(statusCode.stringCode());
    this.httpStatus = httpStatus;
  }

  public CredentialAuthenticationException(
      HttpStatus httpStatus, StatusCode statusCode, Throwable cause) {
    super(statusCode.stringCode(), cause);
    this.httpStatus = httpStatus;
  }

  public HttpStatus httpStatus() {
    return httpStatus;
  }
}
