package app.domain.inquiry;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 관리자가 문의에 남긴 답변입니다. */
@Table(name = "inquiry_replies")
@Entity(name = "inquiry_replies")
public class InquiryReply {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "inquiry_id", nullable = false)
  private Long inquiryId;

  @Column(name = "author_user_id", nullable = false, columnDefinition = "BINARY(16)")
  private UUID authorUserId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected InquiryReply() {}

  private InquiryReply(Long inquiryId, UUID authorUserId, String content, Instant createdAt) {
    this.inquiryId = Objects.requireNonNull(inquiryId, "inquiryId must not be null");
    this.authorUserId = Objects.requireNonNull(authorUserId, "authorUserId must not be null");
    this.content = requireContent(content);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  public static InquiryReply write(Long inquiryId, UUID authorUserId, String content, Instant now) {
    return new InquiryReply(inquiryId, authorUserId, content, now);
  }

  private static String requireContent(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "답변 내용은 필수입니다.");
    }
    return value;
  }

  public Long getId() {
    return id;
  }

  public Long getInquiryId() {
    return inquiryId;
  }

  public UUID getAuthorUserId() {
    return authorUserId;
  }

  public String getContent() {
    return content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
