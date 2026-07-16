package app.domain.auth;

import java.util.Optional;

public interface LoginRefreshTokenRepository {
  Optional<LoginRefreshToken> findByTokenHashForUpdate(String tokenHash);

  LoginRefreshToken save(LoginRefreshToken token);
}
