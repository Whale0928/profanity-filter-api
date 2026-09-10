package app.domain.profanity;

import app.domain.profanity.constant.WordSource;
import app.domain.profanity.constant.isUsedType;
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

@Table(name = "profanity_word")
@Entity(name = "profanity_word")
public class ProfanityWord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String word;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private isUsedType isUsed;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private WordSource source = WordSource.defaultSource();

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "created_by", columnDefinition = "BINARY(16)")
  private UUID createdBy;

  @Column(name = "updated_by", columnDefinition = "BINARY(16)")
  private UUID updatedBy;

  protected ProfanityWord() {}

  public ProfanityWord(Long id, String word, isUsedType isUsed) {
    this(id, word, isUsed, WordSource.defaultSource(), null, null, null, null);
  }

  public ProfanityWord(
      Long id,
      String word,
      isUsedType isUsed,
      WordSource source,
      Instant createdAt,
      Instant updatedAt,
      UUID createdBy,
      UUID updatedBy) {
    this.id = id;
    this.word = word;
    this.isUsed = isUsed;
    this.source = source == null ? WordSource.defaultSource() : source;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.createdBy = createdBy;
    this.updatedBy = updatedBy;
  }

  public static ProfanityWord create(String word) {
    return new ProfanityWord(null, word, isUsedType.Y);
  }

  /**
   * 출처와 작업자를 남기며 사전 단어를 만듭니다.
   *
   * @param source 등록 경로. null이면 UNKNOWN으로 저장합니다.
   * @param actorId 등록한 관리자 식별자. 알 수 없으면 null입니다.
   */
  public static ProfanityWord create(String word, WordSource source, UUID actorId, Instant now) {
    Instant requiredNow = Objects.requireNonNull(now, "now must not be null");
    return new ProfanityWord(
        null, requireWord(word), isUsedType.Y, source, requiredNow, requiredNow, actorId, actorId);
  }

  /** 단어 표현을 변경합니다. */
  public void rename(String word, UUID actorId, Instant now) {
    this.word = requireWord(word);
    touch(actorId, now);
  }

  /** 사용 여부를 변경합니다. N으로 바꾸면 다음 동기화부터 Trie에서 제외됩니다. */
  public void changeUsage(isUsedType isUsed, UUID actorId, Instant now) {
    this.isUsed = Objects.requireNonNull(isUsed, "isUsed must not be null");
    touch(actorId, now);
  }

  private void touch(UUID actorId, Instant now) {
    this.updatedBy = actorId;
    this.updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  public boolean isUsed() {
    return isUsed == isUsedType.Y;
  }

  private static String requireWord(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("word must not be blank");
    }
    return value.trim();
  }

  public Long getId() {
    return id;
  }

  public String getWord() {
    return word;
  }

  public isUsedType getIsUsed() {
    return isUsed;
  }

  public WordSource getSource() {
    return source;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public UUID getUpdatedBy() {
    return updatedBy;
  }
}
