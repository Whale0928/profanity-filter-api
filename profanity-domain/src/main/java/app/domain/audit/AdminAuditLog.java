package app.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 관리자 업무 변경 이력입니다. 요청 원문과 응답, 인증정보는 저장하지 않고 무엇을 어떤 대상에 대해 했는지만 남깁니다. */
@Table(name = "admin_audit_logs")
@Entity(name = "admin_audit_logs")
public class AdminAuditLog {

  private static final int REASON_MAX_LENGTH = 500;
  private static final int ACTION_MAX_LENGTH = 60;
  private static final int TARGET_TYPE_MAX_LENGTH = 60;
  private static final int TARGET_ID_MAX_LENGTH = 100;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "actor_user_id", nullable = false, columnDefinition = "BINARY(16)")
  private UUID actorUserId;

  @Column(nullable = false, length = ACTION_MAX_LENGTH)
  private String action;

  @Column(name = "target_type", nullable = false, length = TARGET_TYPE_MAX_LENGTH)
  private String targetType;

  @Column(name = "target_id", nullable = false, length = TARGET_ID_MAX_LENGTH)
  private String targetId;

  @Column(length = REASON_MAX_LENGTH)
  private String reason;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AdminAuditLog() {}

  private AdminAuditLog(
      UUID actorUserId,
      String action,
      String targetType,
      String targetId,
      String reason,
      Instant createdAt) {
    this.actorUserId = actorUserId;
    this.action = action;
    this.targetType = targetType;
    this.targetId = targetId;
    this.reason = reason;
    this.createdAt = createdAt;
  }

  public static AdminAuditLog record(
      UUID actorUserId,
      String action,
      String targetType,
      String targetId,
      String reason,
      Instant now) {
    return new AdminAuditLog(
        Objects.requireNonNull(actorUserId, "actorUserId must not be null"),
        require(action, "action", ACTION_MAX_LENGTH),
        require(targetType, "targetType", TARGET_TYPE_MAX_LENGTH),
        require(targetId, "targetId", TARGET_ID_MAX_LENGTH),
        truncate(reason, REASON_MAX_LENGTH),
        Objects.requireNonNull(now, "now must not be null"));
  }

  private static String require(String value, String name, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    String trimmed = value.trim();
    if (trimmed.length() > maxLength) {
      throw new IllegalArgumentException(name + " must not exceed " + maxLength + " characters");
    }
    return trimmed;
  }

  private static String truncate(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }

  public Long getId() {
    return id;
  }

  public UUID getActorUserId() {
    return actorUserId;
  }

  public String getAction() {
    return action;
  }

  public String getTargetType() {
    return targetType;
  }

  public String getTargetId() {
    return targetId;
  }

  public String getReason() {
    return reason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
