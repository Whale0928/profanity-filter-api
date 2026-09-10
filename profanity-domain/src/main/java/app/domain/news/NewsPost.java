package app.domain.news;

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
 * 개발자 포털에 노출하는 소식입니다.
 *
 * <p>publishedAt은 status가 PUBLISHED인 동안에만 값을 가집니다. 게시한 소식을 다시 임시 저장으로 되돌리면 비공개 전환이므로 게시 시각도 함께
 * 비웁니다.
 */
@Table(name = "news_posts")
@Entity(name = "news_posts")
public class NewsPost {

  private static final int MAX_TITLE_LENGTH = 160;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = MAX_TITLE_LENGTH)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private NewsCategory category;

  @Column(name = "content_markdown", nullable = false, columnDefinition = "MEDIUMTEXT")
  private String contentMarkdown;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private NewsStatus status;

  @Column(name = "created_by", nullable = false, columnDefinition = "BINARY(16)")
  private UUID createdBy;

  @Column(name = "updated_by", nullable = false, columnDefinition = "BINARY(16)")
  private UUID updatedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  protected NewsPost() {}

  private NewsPost(
      String title,
      NewsCategory category,
      String contentMarkdown,
      NewsStatus status,
      UUID actorId,
      Instant now) {
    this.title = requireTitle(title);
    this.category = Objects.requireNonNull(category, "category must not be null");
    this.contentMarkdown = requireContent(contentMarkdown);
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.createdBy = Objects.requireNonNull(actorId, "actorId must not be null");
    this.updatedBy = actorId;
    this.createdAt = Objects.requireNonNull(now, "now must not be null");
    this.updatedAt = now;
    this.publishedAt = status == NewsStatus.PUBLISHED ? now : null;
  }

  /** 관리자가 새 소식을 작성합니다. */
  public static NewsPost write(
      String title,
      NewsCategory category,
      String contentMarkdown,
      NewsStatus status,
      UUID actorId,
      Instant now) {
    return new NewsPost(title, category, contentMarkdown, status, actorId, now);
  }

  /** 관리자가 소식을 수정합니다. 공개 전환과 비공개 전환에 따라 게시 시각도 함께 조정합니다. */
  public void modify(
      String title,
      NewsCategory category,
      String contentMarkdown,
      NewsStatus status,
      UUID actorId,
      Instant now) {
    Instant requiredNow = Objects.requireNonNull(now, "now must not be null");
    this.title = requireTitle(title);
    this.category = Objects.requireNonNull(category, "category must not be null");
    this.contentMarkdown = requireContent(contentMarkdown);
    this.updatedBy = Objects.requireNonNull(actorId, "actorId must not be null");
    this.updatedAt = requiredNow;
    applyStatus(Objects.requireNonNull(status, "status must not be null"), requiredNow);
  }

  private void applyStatus(NewsStatus next, Instant now) {
    if (next == NewsStatus.PUBLISHED) {
      if (this.publishedAt == null) {
        this.publishedAt = now;
      }
    } else {
      this.publishedAt = null;
    }
    this.status = next;
  }

  public boolean isPublished() {
    return status == NewsStatus.PUBLISHED;
  }

  private static String requireTitle(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "소식 제목은 필수입니다.");
    }
    String trimmed = value.trim();
    if (trimmed.length() > MAX_TITLE_LENGTH) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "소식 제목은 최대 160자까지 가능합니다.");
    }
    return trimmed;
  }

  private static String requireContent(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "소식 본문은 필수입니다.");
    }
    return value;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public NewsCategory getCategory() {
    return category;
  }

  public String getContentMarkdown() {
    return contentMarkdown;
  }

  public NewsStatus getStatus() {
    return status;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public UUID getUpdatedBy() {
    return updatedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }
}
