package app.storage.rds;

import app.domain.auth.LoginRefreshSession;
import app.domain.auth.LoginRefreshSessionRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaLoginRefreshSessionRepository
    extends LoginRefreshSessionRepository, JpaRepository<LoginRefreshSession, UUID> {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from login_refresh_sessions s where s.id = :id")
  Optional<LoginRefreshSession> findByIdForUpdate(@Param("id") UUID id);
}
