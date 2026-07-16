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
}
