package app.domain.auth;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@ToString(of = {"id", "userId", "absoluteExpiresAt", "revokedAt", "revokeReason"})
@EqualsAndHashCode(of = "id")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
@Entity(name = "login_refresh_sessions")
@Table(name = "login_refresh_sessions")
public class LoginRefreshSession {

  @Id
  @Builder.Default
  @Column(columnDefinition = "BINARY(16)")
  private UUID id = UUID.randomUUID();

  @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
  private UUID userId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "absolute_expires_at", nullable = false)
  private Instant absoluteExpiresAt;

  @Column(name = "last_rotated_at", nullable = false)
  private Instant lastRotatedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "revoke_reason", length = 50)
  private RefreshSessionRevocationReason revokeReason;

  public static LoginRefreshSession create(UUID userId, Instant now, Duration absoluteTimeToLive) {
    Objects.requireNonNull(userId, "userId must not be null");
    Instant requiredNow = Objects.requireNonNull(now, "now must not be null");
    requirePositive(absoluteTimeToLive, "absoluteTimeToLive");
    return LoginRefreshSession.builder()
        .userId(userId)
        .createdAt(requiredNow)
        .absoluteExpiresAt(requiredNow.plus(absoluteTimeToLive))
        .lastRotatedAt(requiredNow)
        .build();
  }

  public boolean isExpired(Instant now) {
    return !Objects.requireNonNull(now, "now must not be null").isBefore(absoluteExpiresAt);
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public void markRotated(Instant now) {
    this.lastRotatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  public void revoke(Instant now, RefreshSessionRevocationReason reason) {
    if (revokedAt == null) {
      this.revokedAt = Objects.requireNonNull(now, "now must not be null");
      this.revokeReason = Objects.requireNonNull(reason, "reason must not be null");
    }
  }

  private static void requirePositive(Duration duration, String name) {
    if (duration == null || duration.isZero() || duration.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }
}
