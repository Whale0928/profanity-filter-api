package app.domain.whitelist;

import app.application.filter.ProfanityText;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 계정이 소유한 허용 단어 그룹입니다. 필터 요청이 이 그룹의 ID를 지정하면 여기 등록된 단어는 검출과 마스킹에서 빠집니다.
 *
 * <p>단어는 고객이 입력한 그대로 보관하고, 비교할 때만 {@link ProfanityText#comparisonKey}로 정리합니다. 고객은 응답에서 본 원문 조각을
 * 등록하고 필터는 사전 단어로 검출하므로 두 값을 같은 규칙으로 정리해야 서로 맞습니다.
 */
@Entity(name = "whitelists")
@Table(name = "whitelists")
public class Whitelist {

  public static final int MAX_NAME_LENGTH = 60;
  public static final int MAX_WORDS = 200;
  public static final int MAX_WORD_LENGTH = 80;

  @Id
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
  private UUID userId;

  @Column(nullable = false, length = MAX_NAME_LENGTH)
  private String name;

  @Convert(converter = WhitelistWordsConverter.class)
  @Column(nullable = false, columnDefinition = "TEXT")
  private List<String> words;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Whitelist() {}

  private Whitelist(UUID userId, String name, List<String> words, Instant now) {
    this.id = UUID.randomUUID();
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.name = requireName(name);
    this.words = cleanWords(words);
    this.createdAt = Objects.requireNonNull(now, "now must not be null");
    this.updatedAt = now;
  }

  public static Whitelist create(UUID userId, String name, List<String> words, Instant now) {
    return new Whitelist(userId, name, words, now);
  }

  /** 이름과 단어 목록을 한 번에 바꿉니다. 그룹 ID는 바뀌지 않으므로 이미 연동된 요청은 그대로 동작합니다. */
  public void update(String name, List<String> words, Instant now) {
    this.name = requireName(name);
    this.words = cleanWords(words);
    this.updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  public boolean isOwnedBy(UUID userId) {
    return userId != null && userId.equals(this.userId);
  }

  /** 필터가 비교에 쓰는 키 집합입니다. */
  public Set<String> comparisonKeys() {
    return words.stream().map(ProfanityText::comparisonKey).collect(Collectors.toUnmodifiableSet());
  }

  private static String requireName(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "그룹 이름은 필수입니다.");
    }
    String trimmed = value.trim();
    if (trimmed.length() > MAX_NAME_LENGTH) {
      throw new BusinessException(
          StatusCode.BAD_REQUEST, "그룹 이름은 최대 " + MAX_NAME_LENGTH + "자까지 가능합니다.");
    }
    return trimmed;
  }

  /**
   * 입력한 단어를 정리합니다. 앞뒤 공백을 지우고 빈 값과 비교 키가 같은 중복을 뺀 뒤 입력한 순서를 유지합니다.
   *
   * @throws BusinessException 단어 수나 길이가 상한을 넘거나, 필터가 절대 검출하지 않는 형태인 경우
   */
  private static List<String> cleanWords(List<String> values) {
    if (values == null) {
      return List.of();
    }
    List<String> cleaned = new ArrayList<>();
    Set<String> seenKeys = new HashSet<>();
    for (String value : values) {
      if (value == null || value.isBlank()) {
        continue;
      }
      String word = value.trim();
      if (word.length() > MAX_WORD_LENGTH) {
        throw new BusinessException(
            StatusCode.BAD_REQUEST, "허용 단어는 최대 " + MAX_WORD_LENGTH + "자까지 가능합니다.");
      }
      if (word.contains("\n") || word.contains("\r")) {
        throw new BusinessException(StatusCode.BAD_REQUEST, "허용 단어에는 줄바꿈을 넣을 수 없습니다.");
      }
      String key = ProfanityText.comparisonKey(word);
      if (key.isEmpty()) {
        // 필터는 한글과 영문만 보고 검출하므로 숫자나 기호만으로 된 단어는 등록해도 효과가 없다.
        throw new BusinessException(
            StatusCode.BAD_REQUEST, "한글이나 영문이 없는 단어는 허용 단어로 등록할 수 없습니다: " + word);
      }
      if (seenKeys.add(key)) {
        cleaned.add(word);
      }
    }
    if (cleaned.size() > MAX_WORDS) {
      throw new BusinessException(
          StatusCode.WHITELIST_LIMIT_EXCEEDED, "허용 단어는 그룹당 최대 " + MAX_WORDS + "개까지 가능합니다.");
    }
    return List.copyOf(cleaned);
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getName() {
    return name;
  }

  public List<String> getWords() {
    return words;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
