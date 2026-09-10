package app.security.authentication;

import app.domain.user.UserRole;
import java.util.UUID;

/**
 * SSO 로그인 후 발급한 JWT로 인증된 사람 사용자의 주체입니다.
 *
 * <p>role은 토큰 클레임이 아니라 인증마다 조회한 DB 값이므로, 역할 회수가 다음 요청부터 즉시 반영됩니다.
 */
public record LoginUserPrincipal(UUID id, String email, UserRole role) implements ServicePrincipal {

  @Override
  public AuthenticationType authenticationType() {
    return AuthenticationType.LOGIN_JWT;
  }

  public boolean isAdmin() {
    return role == UserRole.ADMIN;
  }
}
