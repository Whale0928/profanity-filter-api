package app.domain.apikey;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import app.domain.client.PermissionsType;
import app.domain.client.PermissionsTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder(access = PRIVATE)
@ToString(of = {"id", "userId", "name", "email", "keyHint"})
@EqualsAndHashCode(of = "id")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
@Entity(name = "api_keys")
@Table(name = "api_keys")
public class ApiKey {

  @Id
  @Builder.Default
  @Column(columnDefinition = "BINARY(16)")
  private UUID id = UUID.randomUUID();

  @Column(name = "user_id", columnDefinition = "BINARY(16)")
  private UUID userId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String email;

  @Column(
      name = "key_hash",
      nullable = false,
      unique = true,
      length = 64,
      columnDefinition = "CHAR(64)")
  private String keyHash;

  @Column(name = "key_hint", nullable = false, length = 32)
  private String keyHint;

  @Column(name = "issuer_info", nullable = false)
  private String issuerInfo;

  @Column private String note;

  @Builder.Default
  @Convert(converter = PermissionsTypeConverter.class)
  @Column(nullable = false, columnDefinition = "TEXT")
  private List<PermissionsType> permissions = PermissionsType.defaultPermissions();

  @Column(name = "issued_at", nullable = false)
  private LocalDateTime issuedAt;

  @Column(name = "expired_at")
  private LocalDateTime expiredAt;

  @Builder.Default
  @Column(name = "request_count", nullable = false)
  private Long requestCount = 0L;

  @Column(name = "last_used_at")
  private LocalDateTime lastUsedAt;

  @Column(name = "revoked_by", columnDefinition = "BINARY(16)")
  private UUID revokedBy;

  @Column(name = "revocation_reason", length = 500)
  private String revocationReason;

  public static ApiKey issue(
      UUID userId,
      String name,
      String email,
      String keyHash,
      String keyHint,
      String issuerInfo,
      String note,
      LocalDateTime issuedAt) {
    return ApiKey.builder()
        .userId(Objects.requireNonNull(userId, "userId must not be null"))
        .name(requireText(name, "name"))
        .email(requireText(email, "email").toLowerCase(Locale.ROOT))
        .keyHash(requireText(keyHash, "keyHash"))
        .keyHint(requireText(keyHint, "keyHint"))
        .issuerInfo(requireText(issuerInfo, "issuerInfo"))
        .note(blankToNull(note))
        .issuedAt(Objects.requireNonNull(issuedAt, "issuedAt must not be null"))
        .build();
  }

  public boolean isActive() {
    return expiredAt == null;
  }

  public void expire(LocalDateTime now) {
    if (expiredAt == null) {
      expiredAt = Objects.requireNonNull(now, "now must not be null");
    }
  }

  /**
   * 관리자가 API Key를 폐기합니다. 사용자 만료와 같은 만료 시각을 사용하되 폐기자와 사유를 함께 남깁니다.
   *
   * @throws IllegalStateException 이미 만료 또는 폐기된 키인 경우
   */
  public void revokeByAdmin(UUID actorId, String reason, LocalDateTime now) {
    if (!isActive()) {
      throw new IllegalStateException("Expired API key cannot be revoked again");
    }
    this.expiredAt = Objects.requireNonNull(now, "now must not be null");
    this.revokedBy = Objects.requireNonNull(actorId, "actorId must not be null");
    this.revocationReason = truncate(blankToNull(reason), 500);
  }

  /**
   * 마지막 사용 시각을 다시 기록할 때가 되었는지 확인합니다. 상태를 바꾸지 않으므로 읽기 트랜잭션에서 호출해도 안전합니다.
   *
   * @param threshold 최소 기록 간격
   */
  public boolean isUsageRecordStale(LocalDateTime now, Duration threshold) {
    Objects.requireNonNull(now, "now must not be null");
    Objects.requireNonNull(threshold, "threshold must not be null");
    return lastUsedAt == null || !lastUsedAt.plus(threshold).isAfter(now);
  }

  /**
   * 인증에 사용된 시각을 기록합니다. 인증 경로의 쓰기 부하를 억제하기 위해 마지막 기록에서 threshold 이상 지난 경우에만 갱신합니다.
   *
   * @return 실제로 갱신되었으면 true
   */
  public boolean markUsedAt(LocalDateTime now, Duration threshold) {
    if (!isUsageRecordStale(now, threshold)) {
      return false;
    }
    this.lastUsedAt = now;
    return true;
  }

  public ApiKey reissue(String replacementHash, String replacementHint, LocalDateTime now) {
    if (!isActive()) {
      throw new IllegalStateException("Expired API key cannot be reissued");
    }
    expire(now);
    return issue(userId, name, email, replacementHash, replacementHint, issuerInfo, note, now);
  }

  public List<String> plainPermissions() {
    return permissions.stream().map(PermissionsType::getValue).toList();
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.trim();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String truncate(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }
}
