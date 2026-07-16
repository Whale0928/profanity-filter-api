package app.security.authentication;

/** 요청을 인증한 자격 증명의 종류입니다. */
public enum AuthenticationType {
  API_KEY,
  LOGIN_JWT,
  OAUTH2_ACCESS_TOKEN;

  public String authority() {
    return "AUTH_" + name();
  }
}
