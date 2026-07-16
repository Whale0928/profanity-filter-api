package app.domain.auth;

public enum RefreshSessionRevocationReason {
  TOKEN_REUSE_DETECTED,
  USER_INACTIVE,
  ABSOLUTE_EXPIRATION
}
