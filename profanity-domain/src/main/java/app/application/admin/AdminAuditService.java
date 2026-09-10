package app.application.admin;

import app.domain.audit.AdminAuditLog;
import app.domain.audit.AdminAuditLogRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 업무 변경을 감사 기록으로 남깁니다.
 *
 * <p>업무 변경과 같은 트랜잭션에 기록해야 하므로 호출자의 트랜잭션에 반드시 참여합니다. 트랜잭션 밖에서 호출하면 즉시 예외가 발생하며, 이는 감사 기록만 남고 업무 변경이
 * 롤백되는 상황을 막기 위한 의도된 제약입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminAuditService {

  private final AdminAuditLogRepository adminAuditLogRepository;

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(
      UUID actorId, String action, String targetType, String targetId, String reason, Instant now) {
    adminAuditLogRepository.save(
        AdminAuditLog.record(actorId, action, targetType, targetId, reason, now));
  }
}
