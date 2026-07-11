package app.domain.user;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository {
  Optional<UserAccount> findById(UUID id);

  Optional<UserAccount> findByIdForUpdate(UUID id);

  UserAccount save(UserAccount userAccount);
}
