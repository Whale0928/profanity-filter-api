package app.security.login;

import app.core.data.response.constant.StatusCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;

@Getter
public class LoginFlowException extends AuthenticationException {
  private final StatusCode statusCode;
  private final HttpStatus httpStatus;
  private final boolean expireRefreshCookie;

  public LoginFlowException(StatusCode statusCode, HttpStatus httpStatus) {
    this(statusCode, httpStatus, false);
  }

  public LoginFlowException(
      StatusCode statusCode, HttpStatus httpStatus, boolean expireRefreshCookie) {
    super(statusCode.name());
    this.statusCode = statusCode;
    this.httpStatus = httpStatus;
    this.expireRefreshCookie = expireRefreshCookie;
  }
}
