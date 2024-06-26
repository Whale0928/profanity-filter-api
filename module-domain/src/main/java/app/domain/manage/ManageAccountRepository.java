package app.domain.manage;

import java.util.Optional;

public interface ManageAccountRepository {
    Optional<ManageAccount> findById(Long id);

    Optional<ManageAccount> findByPassword(String  id);
}
