package app.storage.rds;

import app.domain.audit.AdminAuditLog;
import app.domain.audit.AdminAuditLogRepository;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAdminAuditLogRepository
    extends AdminAuditLogRepository, JpaRepository<AdminAuditLog, Long> {

  @Override
  @Query("select count(l) from admin_audit_logs l where l.createdAt >= :from")
  long countCreatedSince(@Param("from") Instant from);
}
