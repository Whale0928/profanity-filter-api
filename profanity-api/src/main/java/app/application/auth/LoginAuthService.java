package app.application.auth;

import app.core.data.response.constant.StatusCode;
import app.domain.user.OAuthLoginProfile;
import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import app.security.jwt.IssuedAccessToken;
import app.security.jwt.LoginJwtService;
import app.security.login.LoginFlowException;
import app.security.login.LoginSessionProperties;
import app.security.login.SecureOpaqueTokenService;
import app.security.login.SecureOpaqueTokenService.OpaqueToken;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAuthService implements SsoLoginCompletionService {
  private final SsoAccountService ssoAccountService;
  private final LoginExchangeCodeService exchangeCodeService;
  private final LoginRefreshTokenService refreshTokenService;
  private final UserAccountRepository userAccountRepository;
  private final SecureOpaqueTokenService opaqueTokenService;
  private final LoginJwtService loginJwtService;
  private final LoginSessionProperties properties;
  private final Clock loginAuthClock;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  public String issueExchangeCode(OAuthLoginProfile profile) {
    Instant now = loginAuthClock.instant();
    UserAccount userAccount = ssoAccountService.upsert(profile, now);
    eventPublisher.publishEvent(
        new ApiKeyOwnershipClaimRequested(userAccount.getId(), userAccount.getPrimaryEmail()));
    OpaqueToken exchangeCode = opaqueTokenService.generate();
    exchangeCodeService.issue(
        userAccount.getId(), exchangeCode.hash(), now, properties.exchangeCodeTtl());
    return exchangeCode.plaintext();
  }

  @Transactional
  public LoginTokenBundle exchange(String plaintextCode) {
    Instant now = loginAuthClock.instant();
    LoginExchangeCodeConsumeResult result;
    try {
      result = exchangeCodeService.consume(opaqueTokenService.hash(plaintextCode), now);
    } catch (IllegalArgumentException exception) {
      throw invalidLoginCode();
    }
    if (!result.isConsumed()) {
      throw switch (result.status()) {
        case USER_INACTIVE -> inactiveUser(false);
        case INVALID, EXPIRED, ALREADY_CONSUMED -> invalidLoginCode();
        case CONSUMED -> new IllegalStateException("Consumed exchange code has no user");
      };
    }

    UserAccount userAccount = requireActiveUser(result.userId(), false);
    OpaqueToken refreshToken = opaqueTokenService.generate();
    var session =
        refreshTokenService.createSession(
            userAccount.getId(),
            refreshToken.hash(),
            now,
            properties.refreshTokenTtl(),
            properties.absoluteSessionTtl());
    IssuedAccessToken accessToken = loginJwtService.issue(userAccount);
    return bundle(accessToken, refreshToken, session.tokenExpiresAt(), userAccount, now);
  }

  @Transactional
  public LoginTokenBundle refresh(String plaintextRefreshToken) {
    Instant now = loginAuthClock.instant();
    OpaqueToken replacement = opaqueTokenService.generate();
    LoginRefreshRotationResult result;
    try {
      result =
          refreshTokenService.rotate(
              opaqueTokenService.hash(plaintextRefreshToken),
              replacement.hash(),
              now,
              properties.refreshTokenTtl(),
              properties.refreshReuseGrace());
    } catch (IllegalArgumentException exception) {
      throw invalidRefreshToken(true);
    }

    if (!result.isRotated()) {
      throw switch (result.status()) {
        case REUSED_WITHIN_GRACE -> reusedRefreshToken(false);
        case REUSE_DETECTED_SESSION_REVOKED -> reusedRefreshToken(true);
        case USER_INACTIVE_SESSION_REVOKED -> inactiveUser(true);
        case INVALID_TOKEN, TOKEN_EXPIRED, SESSION_EXPIRED, SESSION_REVOKED ->
            invalidRefreshToken(true);
        case ROTATED -> new IllegalStateException("Rotated refresh token has no user");
      };
    }

    UserAccount userAccount = requireActiveUser(result.userId(), true);
    IssuedAccessToken accessToken = loginJwtService.issue(userAccount);
    return bundle(accessToken, replacement, result.replacementExpiresAt(), userAccount, now);
  }

  public UserAccount currentUser(UUID userId) {
    return requireActiveUser(userId, false);
  }

  private LoginTokenBundle bundle(
      IssuedAccessToken accessToken,
      OpaqueToken refreshToken,
      Instant refreshExpiresAt,
      UserAccount userAccount,
      Instant now) {
    long accessExpiresIn = Math.max(1, Duration.between(now, accessToken.expiresAt()).toSeconds());
    Duration refreshMaxAge = Duration.between(now, refreshExpiresAt);
    return new LoginTokenBundle(
        accessToken.token(), accessExpiresIn, refreshToken.plaintext(), refreshMaxAge, userAccount);
  }

  private UserAccount requireActiveUser(UUID userId, boolean expireRefreshCookie) {
    UserAccount userAccount =
        userAccountRepository
            .findById(userId)
            .orElseThrow(() -> invalidRefreshToken(expireRefreshCookie));
    if (!userAccount.isActive()) {
      throw inactiveUser(expireRefreshCookie);
    }
    return userAccount;
  }

  private LoginFlowException invalidLoginCode() {
    return new LoginFlowException(StatusCode.LOGIN_CODE_INVALID, HttpStatus.UNAUTHORIZED);
  }

  private LoginFlowException invalidRefreshToken(boolean expireCookie) {
    return new LoginFlowException(
        StatusCode.REFRESH_TOKEN_INVALID, HttpStatus.UNAUTHORIZED, expireCookie);
  }

  private LoginFlowException reusedRefreshToken(boolean expireCookie) {
    return new LoginFlowException(
        StatusCode.REFRESH_TOKEN_REUSED, HttpStatus.UNAUTHORIZED, expireCookie);
  }

  private LoginFlowException inactiveUser(boolean expireCookie) {
    return new LoginFlowException(StatusCode.USER_INACTIVE, HttpStatus.FORBIDDEN, expireCookie);
  }

  public record LoginTokenBundle(
      String accessToken,
      long accessExpiresIn,
      String refreshToken,
      Duration refreshMaxAge,
      UserAccount userAccount) {
    @Override
    public String toString() {
      return "LoginTokenBundle[tokens=redacted, userId=" + userAccount.getId() + "]";
    }
  }
}
