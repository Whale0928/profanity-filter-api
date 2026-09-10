package app.storage.rds;

import app.domain.audit.AdminAuditLog;
import app.domain.audit.AdminAuditLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAdminAuditLogRepository
    extends AdminAuditLogRepository, JpaRepository<AdminAuditLog, Long> {}
