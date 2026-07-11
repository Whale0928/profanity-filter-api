package app.domain.auth;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
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
@ToString(of = {"id", "userId", "expiresAt", "consumedAt"})
@EqualsAndHashCode(of = "id")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
@Entity(name = "login_exchange_codes")
@Table(name = "login_exchange_codes")
public class LoginExchangeCode {

  @Id
  @Builder.Default
  @Column(columnDefinition = "BINARY(16)")
  private UUID id = UUID.randomUUID();

  @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
  private UUID userId;

  @Column(name = "code_hash", nullable = false, unique = true, columnDefinition = "CHAR(64)")
  private String codeHash;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "consumed_at")
  private Instant consumedAt;

  public static LoginExchangeCode issue(
      UUID userId, Sha256Hash codeHash, Instant now, Duration timeToLive) {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(codeHash, "codeHash must not be null");
    Instant requiredNow = Objects.requireNonNull(now, "now must not be null");
    requirePositive(timeToLive, "timeToLive");
    return LoginExchangeCode.builder()
        .userId(userId)
        .codeHash(codeHash.value())
        .createdAt(requiredNow)
        .expiresAt(requiredNow.plus(timeToLive))
        .build();
  }

  public boolean isExpired(Instant now) {
    return !Objects.requireNonNull(now, "now must not be null").isBefore(expiresAt);
  }

  public boolean isConsumed() {
    return consumedAt != null;
  }

  public void consume(Instant now) {
    if (isConsumed()) {
      throw new IllegalStateException("Exchange code has already been consumed");
    }
    this.consumedAt = Objects.requireNonNull(now, "now must not be null");
  }

  private static void requirePositive(Duration duration, String name) {
    if (duration == null || duration.isZero() || duration.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }
}
