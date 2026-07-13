package app.application.auth;

public class LoginAccountUnavailableException extends RuntimeException {

  private final Reason reason;

  public LoginAccountUnavailableException(Reason reason) {
    super("Login account is unavailable: " + reason);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }

  public enum Reason {
    USER_NOT_FOUND,
    USER_INACTIVE,
    VERIFIED_EMAIL_REQUIRED,
    AUTHORITATIVE_EMAIL_REQUIRED
  }
}
