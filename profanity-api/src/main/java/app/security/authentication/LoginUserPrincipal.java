package app.security.authentication;

import java.util.UUID;

/** SSO 로그인 후 발급한 JWT로 인증된 사람 사용자의 주체입니다. */
public record LoginUserPrincipal(UUID id, String email) implements ServicePrincipal {

  @Override
  public AuthenticationType authenticationType() {
    return AuthenticationType.LOGIN_JWT;
  }
}
