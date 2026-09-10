package app.domain.inquiry;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 사용자가 등록한 문의입니다.
 *
 * <p>요청자는 두 경로로 들어옵니다. 로그인 사용자가 대시보드에서 등록하면 requesterUserId만 채워지고, 기존 외부 단어 요청 API로 들어오면
 * requesterApiKeyId가 채워집니다. 이때 requesterUserId는 해당 API Key의 소유자가 확인되는 경우에만 함께 채웁니다.
 */
@Table(name = "inquiries")
@Entity(name = "inquiries")
public class Inquiry {

  private static final int MAX_TITLE_LENGTH = 160;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private InquiryType type;

  @Column(nullable = false, length = MAX_TITLE_LENGTH)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "requester_user_id", columnDefinition = "BINARY(16)")
  private UUID requesterUserId;

  @Column(name = "requester_api_key_id", columnDefinition = "BINARY(16)")
  private UUID requesterApiKeyId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private InquiryStatus status;

  @Column(name = "assigned_to", columnDefinition = "BINARY(16)")
  private UUID assignedTo;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  protected Inquiry() {}

  private Inquiry(
      InquiryType type,
      String title,
      String content,
      UUID requesterUserId,
      UUID requesterApiKeyId,
      Instant now) {
    this.type = Objects.requireNonNull(type, "type must not be null");
    this.title = requireTitle(title);
    this.content = requireContent(content);
    this.requesterUserId = requesterUserId;
    this.requesterApiKeyId = requesterApiKeyId;
    this.status = InquiryStatus.RECEIVED;
    this.createdAt = Objects.requireNonNull(now, "now must not be null");
    this.updatedAt = now;
  }

  /** 로그인 사용자가 대시보드에서 등록한 문의입니다. */
  public static Inquiry fromLoginUser(
      InquiryType type, String title, String content, UUID requesterUserId, Instant now) {
    Objects.requireNonNull(requesterUserId, "requesterUserId must not be null");
    return new Inquiry(type, title, content, requesterUserId, null, now);
  }

  /** 기존 외부 단어 요청 API로 들어온 문의입니다. 소유자가 확인되지 않으면 requesterUserId는 비어 있습니다. */
  public static Inquiry fromApiKey(
      InquiryType type,
      String title,
      String content,
      UUID requesterUserId,
      UUID requesterApiKeyId,
      Instant now) {
    Objects.requireNonNull(requesterApiKeyId, "requesterApiKeyId must not be null");
    return new Inquiry(type, title, content, requesterUserId, requesterApiKeyId, now);
  }

  /** 문의 상태를 변경합니다. 완료로 바꿀 때만 완료 시각을 남기고 되돌리면 지웁니다. */
  public void changeStatus(InquiryStatus next, Instant now) {
    Objects.requireNonNull(next, "next must not be null");
    this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    this.resolvedAt = next == InquiryStatus.RESOLVED ? now : null;
    this.status = next;
  }

  /** 관리자가 답변하면 담당자를 기록하고, 접수 상태였다면 처리 중으로 옮깁니다. */
  public void markReplied(UUID adminId, Instant now) {
    this.assignedTo = Objects.requireNonNull(adminId, "adminId must not be null");
    this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    if (this.status == InquiryStatus.RECEIVED) {
      this.status = InquiryStatus.IN_PROGRESS;
    }
  }

  /** 로그인 사용자가 본인이 등록한 문의인지 확인합니다. */
  public boolean isOwnedBy(UUID userId) {
    return userId != null && userId.equals(requesterUserId);
  }

  public boolean isWordRequest() {
    return type == InquiryType.WORD_REQUEST;
  }

  private static String requireTitle(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "문의 제목은 필수입니다.");
    }
    String trimmed = value.trim();
    if (trimmed.length() > MAX_TITLE_LENGTH) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "문의 제목은 최대 160자까지 가능합니다.");
    }
    return trimmed;
  }

  private static String requireContent(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "문의 내용은 필수입니다.");
    }
    return value;
  }

  public Long getId() {
    return id;
  }

  public InquiryType getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public UUID getRequesterUserId() {
    return requesterUserId;
  }

  public UUID getRequesterApiKeyId() {
    return requesterApiKeyId;
  }

  public InquiryStatus getStatus() {
    return status;
  }

  public UUID getAssignedTo() {
    return assignedTo;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }
}
