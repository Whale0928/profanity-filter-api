package app.storage.rds;

import app.domain.auth.LoginExchangeCode;
import app.domain.auth.LoginExchangeCodeRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaLoginExchangeCodeRepository
    extends LoginExchangeCodeRepository, JpaRepository<LoginExchangeCode, UUID> {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from login_exchange_codes c where c.codeHash = :codeHash")
  Optional<LoginExchangeCode> findByCodeHashForUpdate(@Param("codeHash") String codeHash);
}
