package app.storage.rds;

import app.domain.auth.LoginRefreshToken;
import app.domain.auth.LoginRefreshTokenRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaLoginRefreshTokenRepository
    extends LoginRefreshTokenRepository, JpaRepository<LoginRefreshToken, UUID> {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from login_refresh_tokens t where t.tokenHash = :tokenHash")
  Optional<LoginRefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
