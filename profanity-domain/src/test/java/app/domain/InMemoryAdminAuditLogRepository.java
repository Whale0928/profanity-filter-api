package app.domain;

import app.domain.audit.AdminAuditLog;
import app.domain.audit.AdminAuditLogRepository;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAdminAuditLogRepository implements AdminAuditLogRepository {

  private final List<AdminAuditLog> values = new ArrayList<>();
  private final AtomicLong sequence = new AtomicLong();

  @Override
  public AdminAuditLog save(AdminAuditLog auditLog) {
    if (auditLog.getId() == null) {
      assignId(auditLog, sequence.incrementAndGet());
    }
    values.add(auditLog);
    return auditLog;
  }

  @Override
  public List<AdminAuditLog> findAllByTargetTypeAndTargetIdOrderByIdDesc(
      String targetType, String targetId) {
    return values.stream()
        .filter(log -> log.getTargetType().equals(targetType) && log.getTargetId().equals(targetId))
        .sorted(Comparator.comparing(AdminAuditLog::getId).reversed())
        .toList();
  }

  @Override
  public List<AdminAuditLog> findAllByActorUserIdOrderByIdDesc(UUID actorUserId) {
    return values.stream()
        .filter(log -> log.getActorUserId().equals(actorUserId))
        .sorted(Comparator.comparing(AdminAuditLog::getId).reversed())
        .toList();
  }

  public List<AdminAuditLog> findAll() {
    return List.copyOf(values);
  }

  private static void assignId(AdminAuditLog auditLog, long id) {
    try {
      Field field = AdminAuditLog.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(auditLog, id);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
