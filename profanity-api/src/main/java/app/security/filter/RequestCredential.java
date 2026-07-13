package app.security.filter;

import app.security.authentication.AuthenticationType;
import java.util.Objects;

/** 요청에서 추출한 단일 자격 증명입니다. 문자열 표현에는 원문을 노출하지 않습니다. */
public final class RequestCredential {
  private final AuthenticationType type;
  private final String value;

  public RequestCredential(AuthenticationType type, String value) {
    this.type = Objects.requireNonNull(type);
    this.value = Objects.requireNonNull(value);
  }

  public AuthenticationType type() {
    return type;
  }

  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return "RequestCredential[type=" + type + ", value=redacted]";
  }
}
