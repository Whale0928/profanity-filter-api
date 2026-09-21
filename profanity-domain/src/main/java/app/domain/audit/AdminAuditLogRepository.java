package app.domain.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AdminAuditLogRepository {
  AdminAuditLog save(AdminAuditLog auditLog);

  List<AdminAuditLog> findAllByTargetTypeAndTargetIdOrderByIdDesc(
      String targetType, String targetId);

  /** 지정한 시각 이후에 기록된 관리자 작업 수입니다. */
  long countCreatedSince(Instant from);

  List<AdminAuditLog> findAllByActorUserIdOrderByIdDesc(UUID actorUserId);
}
