package app.storage.rds;

import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserAccountRepository
    extends UserAccountRepository, JpaRepository<UserAccount, UUID> {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from users u where u.id = :id")
  Optional<UserAccount> findByIdForUpdate(@Param("id") UUID id);
}
