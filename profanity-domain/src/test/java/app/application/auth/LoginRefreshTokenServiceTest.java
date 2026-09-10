package app.application.auth;

import static org.assertj.core.api.Assertions.assertThat;

import app.domain.auth.LoginRefreshSession;
import app.domain.auth.LoginRefreshSessionRepository;
import app.domain.auth.LoginRefreshToken;
import app.domain.auth.LoginRefreshTokenRepository;
import app.domain.auth.RefreshSessionRevocationReason;
import app.domain.auth.Sha256Hash;
import app.domain.support.PageResult;
import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import app.domain.user.UserRole;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  @Test
  @DisplayName("logout하면 refresh token이 속한 세션을 폐기한다")
  void logout_whenTokenBelongsToSession_revokesSession() {
    LoginRefreshSessionIssue issue = createSession();

    service.logout(INITIAL_HASH, NOW.plusSeconds(1));

    LoginRefreshSession session = sessionRepository.find(issue.sessionId()).orElseThrow();
    assertThat(session.isRevoked()).isTrue();
    assertThat(session.getRevokeReason()).isEqualTo(RefreshSessionRevocationReason.USER_LOGOUT);
  }

  @Test
  @DisplayName("존재하지 않는 refresh token으로 logout해도 예외 없이 멱등하게 처리한다")
  void logout_whenTokenDoesNotExist_doesNothing() {
    LoginRefreshSessionIssue issue = createSession();

    service.logout(REPLACEMENT_HASH, NOW.plusSeconds(1));

    LoginRefreshSession session = sessionRepository.find(issue.sessionId()).orElseThrow();
    assertThat(session.isRevoked()).isFalse();
  }

  @Test
  @DisplayName("이미 폐기된 세션에 logout해도 기존 폐기 사유를 덮어쓰지 않는다")
  void logout_whenSessionAlreadyRevoked_keepsOriginalReason() {
    LoginRefreshSessionIssue issue = createSession();
    service.rotate(INITIAL_HASH, REPLACEMENT_HASH, NOW.plusSeconds(1), REFRESH_TTL, GRACE);
    service.rotate(
        INITIAL_HASH, hash('c'), NOW.plusSeconds(1).plus(GRACE).plusNanos(1), REFRESH_TTL, GRACE);
    LoginRefreshSession revokedSession = sessionRepository.find(issue.sessionId()).orElseThrow();
    assertThat(revokedSession.isRevoked()).isTrue();

    service.logout(REPLACEMENT_HASH, NOW.plusSeconds(10));

    LoginRefreshSession session = sessionRepository.find(issue.sessionId()).orElseThrow();
    assertThat(session.getRevokeReason())
        .isEqualTo(RefreshSessionRevocationReason.TOKEN_REUSE_DETECTED);
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

    @Override
    public List<UserAccount> findAllByIdIn(Collection<UUID> ids) {
      return ids.stream().map(values::get).filter(Objects::nonNull).toList();
    }

    @Override
    public PageResult<UserAccount> searchForAdmin(String query, UserRole role, int page, int size) {
      List<UserAccount> matched =
          values.values().stream()
              .filter(user -> role == null || user.getRole() == role)
              .filter(
                  user ->
                      query == null
                          || user.getDisplayName().contains(query)
                          || user.getPrimaryEmail().contains(query))
              .toList();
      int from = Math.min(page * size, matched.size());
      int to = Math.min(from + size, matched.size());
      return PageResult.of(matched.subList(from, to), page, to < matched.size());
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
