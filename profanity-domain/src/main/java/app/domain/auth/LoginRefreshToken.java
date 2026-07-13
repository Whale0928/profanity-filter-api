package app.domain.auth;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString(of = {"id", "sessionId", "issuedAt", "expiresAt", "consumedAt"})
@EqualsAndHashCode(of = "id")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
@Entity(name = "login_refresh_tokens")
@Table(name = "login_refresh_tokens")
public class LoginRefreshToken {

  @Id
  @Builder.Default
  @Column(columnDefinition = "BINARY(16)")
  private UUID id = UUID.randomUUID();

  @Column(name = "session_id", nullable = false, columnDefinition = "BINARY(16)")
  private UUID sessionId;

  @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "CHAR(64)")
  private String tokenHash;

  @Column(name = "issued_at", nullable = false, updatable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "consumed_at")
  private Instant consumedAt;

  @Column(name = "replaced_by_token_id", columnDefinition = "BINARY(16)")
  private UUID replacedByTokenId;

  public static LoginRefreshToken issue(
      UUID sessionId, Sha256Hash tokenHash, Instant now, Instant expiresAt) {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(tokenHash, "tokenHash must not be null");
    Instant requiredNow = Objects.requireNonNull(now, "now must not be null");
    Instant requiredExpiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    if (!requiredExpiresAt.isAfter(requiredNow)) {
      throw new IllegalArgumentException("expiresAt must be after now");
    }
    return LoginRefreshToken.builder()
        .sessionId(sessionId)
        .tokenHash(tokenHash.value())
        .issuedAt(requiredNow)
        .expiresAt(requiredExpiresAt)
        .build();
  }

  public boolean isExpired(Instant now) {
    return !Objects.requireNonNull(now, "now must not be null").isBefore(expiresAt);
  }

  public boolean isConsumed() {
    return consumedAt != null;
  }

  public boolean wasReusedWithin(Instant now, java.time.Duration gracePeriod) {
    Objects.requireNonNull(now, "now must not be null");
    if (!isConsumed()) {
      return false;
    }
    if (gracePeriod == null || gracePeriod.isNegative()) {
      throw new IllegalArgumentException("gracePeriod must not be negative");
    }
    return !now.isAfter(consumedAt.plus(gracePeriod));
  }

  public void consume(UUID replacementTokenId, Instant now) {
    if (isConsumed()) {
      throw new IllegalStateException("Refresh token has already been consumed");
    }
    this.replacedByTokenId =
        Objects.requireNonNull(replacementTokenId, "replacementTokenId must not be null");
    this.consumedAt = Objects.requireNonNull(now, "now must not be null");
  }
}
