package app.application.auth;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginRefreshTokenServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
  private static final Duration REFRESH_TTL = Duration.ofDays(14);
  private static final Duration ABSOLUTE_TTL = Duration.ofDays(30);
  private static final Duration GRACE = Duration.ofSeconds(3);
  private static final Sha256Hash INITIAL_HASH = hash('a');
  private static final Sha256Hash REPLACEMENT_HASH = hash('b');

  private final InMemoryUserAccountRepository userRepository = new InMemoryUserAccountRepository();
  private final InMemoryRefreshSessionRepository sessionRepository =
      new InMemoryRefreshSessionRepository();
  private final InMemoryRefreshTokenRepository tokenRepository =
      new InMemoryRefreshTokenRepository();
  private final LoginRefreshTokenService service =
      new LoginRefreshTokenService(sessionRepository, tokenRepository, userRepository);
  private UserAccount user;

  @BeforeEach
  void setUp() {
    user = UserAccount.create("Tester", "tester@example.com", null, NOW);
    userRepository.save(user);
  }

  @Test
  @DisplayName("refresh token rotation은 기존 토큰을 소비하고 교체 토큰을 발급한다")
  void rotate_whenCurrentTokenIsValid_rotatesToken() {
    LoginRefreshSessionIssue issue = createSession();

    LoginRefreshRotationResult result =
        service.rotate(INITIAL_HASH, REPLACEMENT_HASH, NOW.plusSeconds(1), REFRESH_TTL, GRACE);

    LoginRefreshToken initial = tokenRepository.find(INITIAL_HASH).orElseThrow();
    assertThat(result.status()).isEqualTo(LoginRefreshRotationStatus.ROTATED);
    assertThat(result.sessionId()).isEqualTo(issue.sessionId());
    assertThat(initial.isConsumed()).isTrue();
    assertThat(initial.getReplacedByTokenId()).isEqualTo(result.replacementTokenId());
    assertThat(tokenRepository.find(REPLACEMENT_HASH)).isPresent();
  }

  @Test
  @DisplayName("소비된 토큰이 grace 안에 재사용되면 family를 유지한다")
  void rotate_whenConsumedTokenReusedWithinGrace_keepsSession() {
    LoginRefreshSessionIssue issue = createSession();
    Instant rotatedAt = NOW.plusSeconds(1);
    service.rotate(INITIAL_HASH, REPLACEMENT_HASH, rotatedAt, REFRESH_TTL, GRACE);

    LoginRefreshRotationResult result =
        service.rotate(INITIAL_HASH, hash('c'), rotatedAt.plus(GRACE), REFRESH_TTL, GRACE);

    assertThat(result.status()).isEqualTo(LoginRefreshRotationStatus.REUSED_WITHIN_GRACE);
    assertThat(sessionRepository.find(issue.sessionId()).orElseThrow().isRevoked()).isFalse();
  }

  @Test
  @DisplayName("소비된 토큰이 grace 이후 재사용되면 token family 전체를 폐기한다")
  void rotate_whenConsumedTokenReusedAfterGrace_revokesSession() {
    LoginRefreshSessionIssue issue = createSession();
    Instant rotatedAt = NOW.plusSeconds(1);
    service.rotate(INITIAL_HASH, REPLACEMENT_HASH, rotatedAt, REFRESH_TTL, GRACE);

    LoginRefreshRotationResult replay =
        service.rotate(
            INITIAL_HASH, hash('c'), rotatedAt.plus(GRACE).plusNanos(1), REFRESH_TTL, GRACE);
    LoginRefreshRotationResult winnerToken =
        service.rotate(
            REPLACEMENT_HASH, hash('d'), rotatedAt.plus(GRACE).plusSeconds(1), REFRESH_TTL, GRACE);

    LoginRefreshSession session = sessionRepository.find(issue.sessionId()).orElseThrow();
    assertThat(replay.status())
        .isEqualTo(LoginRefreshRotationStatus.REUSE_DETECTED_SESSION_REVOKED);
    assertThat(session.isRevoked()).isTrue();
    assertThat(session.getRevokeReason())
        .isEqualTo(RefreshSessionRevocationReason.TOKEN_REUSE_DETECTED);
    assertThat(winnerToken.status()).isEqualTo(LoginRefreshRotationStatus.SESSION_REVOKED);
  }

  @Test
  @DisplayName("사용자가 비활성화되면 refresh session을 폐기한다")
  void rotate_whenUserDisabled_revokesSession() {
    LoginRefreshSessionIssue issue = createSession();
    user.disable(NOW.plusSeconds(1));

    LoginRefreshRotationResult result =
        service.rotate(INITIAL_HASH, REPLACEMENT_HASH, NOW.plusSeconds(2), REFRESH_TTL, GRACE);

    LoginRefreshSession session = sessionRepository.find(issue.sessionId()).orElseThrow();
    assertThat(result.status()).isEqualTo(LoginRefreshRotationStatus.USER_INACTIVE_SESSION_REVOKED);
    assertThat(session.getRevokeReason()).isEqualTo(RefreshSessionRevocationReason.USER_INACTIVE);
  }

  private LoginRefreshSessionIssue createSession() {
    return service.createSession(user.getId(), INITIAL_HASH, NOW, REFRESH_TTL, ABSOLUTE_TTL);
  }

  private static Sha256Hash hash(char value) {
    return new Sha256Hash(String.valueOf(value).repeat(64));
  }

  private static final class InMemoryUserAccountRepository implements UserAccountRepository {
    private final Map<UUID, UserAccount> values = new LinkedHashMap<>();

    @Override
    public Optional<UserAccount> findById(UUID id) {
      return Optional.ofNullable(values.get(id));
    }

    @Override
    public Optional<UserAccount> findByIdForUpdate(UUID id) {
      return findById(id);
    }

    @Override
    public Optional<UserAccount> findByPrimaryEmailForUpdate(String primaryEmail) {
      return values.values().stream()
          .filter(user -> user.getPrimaryEmail().equalsIgnoreCase(primaryEmail.trim()))
          .findFirst();
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
      values.put(userAccount.getId(), userAccount);
      return userAccount;
    }
  }

  private static final class InMemoryRefreshSessionRepository
      implements LoginRefreshSessionRepository {
    private final Map<UUID, LoginRefreshSession> values = new LinkedHashMap<>();

    @Override
    public Optional<LoginRefreshSession> findByIdForUpdate(UUID id) {
      return Optional.ofNullable(values.get(id));
    }

    @Override
    public LoginRefreshSession save(LoginRefreshSession session) {
      values.put(session.getId(), session);
      return session;
    }

    Optional<LoginRefreshSession> find(UUID id) {
      return Optional.ofNullable(values.get(id));
    }
  }

  private static final class InMemoryRefreshTokenRepository implements LoginRefreshTokenRepository {
    private final Map<String, LoginRefreshToken> values = new LinkedHashMap<>();

    @Override
    public Optional<LoginRefreshToken> findByTokenHashForUpdate(String tokenHash) {
      return Optional.ofNullable(values.get(tokenHash));
    }

    @Override
    public LoginRefreshToken save(LoginRefreshToken token) {
      values.put(token.getTokenHash(), token);
      return token;
    }

    Optional<LoginRefreshToken> find(Sha256Hash hash) {
      return Optional.ofNullable(values.get(hash.value()));
    }
  }
}
