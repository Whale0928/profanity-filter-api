package app.application.auth;

import app.domain.auth.LoginRefreshSession;
import app.domain.auth.LoginRefreshSessionRepository;
import app.domain.auth.LoginRefreshToken;
import app.domain.auth.LoginRefreshTokenRepository;
import app.domain.auth.RefreshSessionRevocationReason;
import app.domain.auth.Sha256Hash;
import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginRefreshTokenService {

  private final LoginRefreshSessionRepository sessionRepository;
  private final LoginRefreshTokenRepository tokenRepository;
  private final UserAccountRepository userAccountRepository;

  @Transactional
  public LoginRefreshSessionIssue createSession(
      UUID userId,
      Sha256Hash initialTokenHash,
      Instant now,
      Duration refreshTokenTimeToLive,
      Duration absoluteSessionTimeToLive) {
    requirePositive(refreshTokenTimeToLive, "refreshTokenTimeToLive");
    requirePositive(absoluteSessionTimeToLive, "absoluteSessionTimeToLive");
    requireActiveUser(userId);

    LoginRefreshSession session =
        sessionRepository.save(LoginRefreshSession.create(userId, now, absoluteSessionTimeToLive));
    Instant tokenExpiresAt =
        minimum(now.plus(refreshTokenTimeToLive), session.getAbsoluteExpiresAt());
    LoginRefreshToken token =
        tokenRepository.save(
            LoginRefreshToken.issue(session.getId(), initialTokenHash, now, tokenExpiresAt));
    return new LoginRefreshSessionIssue(
        session.getId(),
        token.getId(),
        userId,
        token.getExpiresAt(),
        session.getAbsoluteExpiresAt());
  }

  @Transactional
  public LoginRefreshRotationResult rotate(
      Sha256Hash currentTokenHash,
      Sha256Hash replacementTokenHash,
      Instant now,
      Duration refreshTokenTimeToLive,
      Duration reuseGracePeriod) {
    requirePositive(refreshTokenTimeToLive, "refreshTokenTimeToLive");
    requireNonNegative(reuseGracePeriod, "reuseGracePeriod");
    if (currentTokenHash.equals(replacementTokenHash)) {
      throw new IllegalArgumentException("Replacement token hash must be different");
    }

    return tokenRepository
        .findByTokenHashForUpdate(currentTokenHash.value())
        .map(
            token ->
                rotateLocked(
                    token, replacementTokenHash, now, refreshTokenTimeToLive, reuseGracePeriod))
        .orElseGet(
            () ->
                LoginRefreshRotationResult.failed(LoginRefreshRotationStatus.INVALID_TOKEN, null));
  }

  private LoginRefreshRotationResult rotateLocked(
      LoginRefreshToken currentToken,
      Sha256Hash replacementTokenHash,
      Instant now,
      Duration refreshTokenTimeToLive,
      Duration reuseGracePeriod) {
    LoginRefreshSession session =
        sessionRepository
            .findByIdForUpdate(currentToken.getSessionId())
            .orElseThrow(
                () -> new IllegalStateException("Refresh token references a missing session"));

    if (session.isRevoked()) {
      return LoginRefreshRotationResult.failed(
          LoginRefreshRotationStatus.SESSION_REVOKED, session.getId());
    }
    if (currentToken.isConsumed()) {
      return handleTokenReuse(currentToken, session, now, reuseGracePeriod);
    }

    UserAccount userAccount = findRequiredUser(session.getUserId());
    if (!userAccount.isActive()) {
      session.revoke(now, RefreshSessionRevocationReason.USER_INACTIVE);
      sessionRepository.save(session);
      return LoginRefreshRotationResult.failed(
          LoginRefreshRotationStatus.USER_INACTIVE_SESSION_REVOKED, session.getId());
    }
    if (session.isExpired(now)) {
      session.revoke(now, RefreshSessionRevocationReason.ABSOLUTE_EXPIRATION);
      sessionRepository.save(session);
      return LoginRefreshRotationResult.failed(
          LoginRefreshRotationStatus.SESSION_EXPIRED, session.getId());
    }
    if (currentToken.isExpired(now)) {
      return LoginRefreshRotationResult.failed(
          LoginRefreshRotationStatus.TOKEN_EXPIRED, session.getId());
    }

    Instant replacementExpiresAt =
        minimum(now.plus(refreshTokenTimeToLive), session.getAbsoluteExpiresAt());
    LoginRefreshToken replacementToken =
        LoginRefreshToken.issue(session.getId(), replacementTokenHash, now, replacementExpiresAt);
    currentToken.consume(replacementToken.getId(), now);
    session.markRotated(now);
    tokenRepository.save(currentToken);
    tokenRepository.save(replacementToken);
    sessionRepository.save(session);
    return LoginRefreshRotationResult.rotated(
        session.getId(),
        session.getUserId(),
        replacementToken.getId(),
        replacementToken.getExpiresAt());
  }

  private LoginRefreshRotationResult handleTokenReuse(
      LoginRefreshToken token,
      LoginRefreshSession session,
      Instant now,
      Duration reuseGracePeriod) {
    if (token.wasReusedWithin(now, reuseGracePeriod)) {
      return LoginRefreshRotationResult.failed(
          LoginRefreshRotationStatus.REUSED_WITHIN_GRACE, session.getId());
    }
    session.revoke(now, RefreshSessionRevocationReason.TOKEN_REUSE_DETECTED);
    sessionRepository.save(session);
    return LoginRefreshRotationResult.failed(
        LoginRefreshRotationStatus.REUSE_DETECTED_SESSION_REVOKED, session.getId());
  }

  private void requireActiveUser(UUID userId) {
    UserAccount userAccount = findRequiredUser(userId);
    if (!userAccount.isActive()) {
      throw new LoginAccountUnavailableException(
          LoginAccountUnavailableException.Reason.USER_INACTIVE);
    }
  }

  private UserAccount findRequiredUser(UUID userId) {
    return userAccountRepository
        .findByIdForUpdate(userId)
        .orElseThrow(
            () ->
                new LoginAccountUnavailableException(
                    LoginAccountUnavailableException.Reason.USER_NOT_FOUND));
  }

  private static Instant minimum(Instant first, Instant second) {
    return first.isBefore(second) ? first : second;
  }

  private static void requirePositive(Duration duration, String name) {
    if (duration == null || duration.isZero() || duration.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }

  private static void requireNonNegative(Duration duration, String name) {
    Objects.requireNonNull(duration, name + " must not be null");
    if (duration.isNegative()) {
      throw new IllegalArgumentException(name + " must not be negative");
    }
  }
}
