package app.domain.user;

/** 개발자 포털 사용자에게 부여되는 역할입니다. 신규 가입자와 기존 사용자는 모두 CLIENT로 시작합니다. */
public enum UserRole {
  CLIENT,
  ADMIN;

  public static UserRole defaultRole() {
    return CLIENT;
  }

  /** Spring Security 권한 문자열로 변환합니다. */
  public String authority() {
    return "ROLE_" + name();
  }
}
