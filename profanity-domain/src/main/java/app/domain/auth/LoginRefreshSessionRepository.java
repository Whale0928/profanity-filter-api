package app.domain.auth;

import java.util.Optional;
import java.util.UUID;

public interface LoginRefreshSessionRepository {
  Optional<LoginRefreshSession> findByIdForUpdate(UUID id);

  LoginRefreshSession save(LoginRefreshSession session);
}
