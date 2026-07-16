package app.application.auth;

public enum LoginExchangeCodeConsumeStatus {
  CONSUMED,
  INVALID,
  EXPIRED,
  ALREADY_CONSUMED,
  USER_INACTIVE
}
