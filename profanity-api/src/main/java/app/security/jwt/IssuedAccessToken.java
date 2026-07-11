package app.security.jwt;

import java.time.Instant;
import java.util.Objects;

/** 발급된 access token입니다. 문자열 표현에는 token 원문을 노출하지 않습니다. */
public final class IssuedAccessToken {
  private final String token;
  private final Instant expiresAt;

  public IssuedAccessToken(String token, Instant expiresAt) {
    this.token = Objects.requireNonNull(token);
    this.expiresAt = Objects.requireNonNull(expiresAt);
  }

  public String token() {
    return token;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  @Override
  public String toString() {
    return "IssuedAccessToken[token=redacted, expiresAt=" + expiresAt + "]";
  }
}
