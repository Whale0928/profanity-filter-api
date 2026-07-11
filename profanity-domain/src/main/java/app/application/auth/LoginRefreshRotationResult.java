package app.application.auth;

import java.time.Instant;
import java.util.UUID;

public record LoginRefreshRotationResult(
    LoginRefreshRotationStatus status,
    UUID sessionId,
    UUID userId,
    UUID replacementTokenId,
    Instant replacementExpiresAt) {

  public static LoginRefreshRotationResult rotated(
      UUID sessionId, UUID userId, UUID replacementTokenId, Instant replacementExpiresAt) {
    return new LoginRefreshRotationResult(
        LoginRefreshRotationStatus.ROTATED,
        sessionId,
        userId,
        replacementTokenId,
        replacementExpiresAt);
  }

  public static LoginRefreshRotationResult failed(
      LoginRefreshRotationStatus status, UUID sessionId) {
    if (status == LoginRefreshRotationStatus.ROTATED) {
      throw new IllegalArgumentException("Use rotated factory for a successful result");
    }
    return new LoginRefreshRotationResult(status, sessionId, null, null, null);
  }

  public boolean isRotated() {
    return status == LoginRefreshRotationStatus.ROTATED;
  }
}
