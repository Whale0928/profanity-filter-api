package app.domain.audit;

import java.util.List;
import java.util.UUID;

public interface AdminAuditLogRepository {
  AdminAuditLog save(AdminAuditLog auditLog);

  List<AdminAuditLog> findAllByTargetTypeAndTargetIdOrderByIdDesc(
      String targetType, String targetId);

  List<AdminAuditLog> findAllByActorUserIdOrderByIdDesc(UUID actorUserId);
}
