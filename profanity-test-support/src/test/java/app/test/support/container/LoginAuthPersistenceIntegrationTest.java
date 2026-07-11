package app.test.support.container;

import static org.assertj.core.api.Assertions.assertThat;

import app.application.auth.LoginExchangeCodeConsumeResult;
import app.application.auth.LoginExchangeCodeConsumeStatus;
import app.application.auth.LoginExchangeCodeService;
import app.application.auth.LoginRefreshRotationResult;
import app.application.auth.LoginRefreshRotationStatus;
import app.application.auth.LoginRefreshSessionIssue;
import app.application.auth.LoginRefreshTokenService;
import app.application.auth.SsoAccountService;
import app.application.auth.SsoAccountTransactionService;
import app.domain.auth.RefreshSessionRevocationReason;
import app.domain.auth.Sha256Hash;
import app.domain.user.OAuthLoginProfile;
import app.domain.user.OAuthProvider;
import app.domain.user.UserAccount;
import app.storage.rds.JpaLoginExchangeCodeRepository;
import app.storage.rds.JpaLoginRefreshSessionRepository;
import app.storage.rds.JpaLoginRefreshTokenRepository;
import app.storage.rds.JpaOAuthAccountRepository;
import app.storage.rds.JpaUserAccountRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest(
    classes = LoginAuthPersistenceIntegrationTest.TestApplication.class,
    properties = {
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.jpa.open-in-view=false",
      "spring.flyway.enabled=true"
    })
class LoginAuthPersistenceIntegrationTest {

  private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
  private static final Duration REFRESH_TTL = Duration.ofDays(14);
  private static final Duration ABSOLUTE_TTL = Duration.ofDays(30);
  private static final Duration GRACE = Duration.ofSeconds(3);

  @Container private static final MySQLContainer MYSQL = MySqlTestContainer.create();

  @Autowired private SsoAccountService ssoAccountService;
  @Autowired private LoginExchangeCodeService exchangeCodeService;
  @Autowired private LoginRefreshTokenService refreshTokenService;
  @Autowired private JpaUserAccountRepository userRepository;
  @Autowired private JpaOAuthAccountRepository oauthRepository;
  @Autowired private JpaLoginExchangeCodeRepository exchangeCodeRepository;
  @Autowired private JpaLoginRefreshSessionRepository sessionRepository;
  @Autowired private JpaLoginRefreshTokenRepository tokenRepository;

  private ExecutorService executor;

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
  }

  @BeforeEach
  void setUp() {
    executor = Executors.newFixedThreadPool(2);
    tokenRepository.deleteAllInBatch();
    sessionRepository.deleteAllInBatch();
    exchangeCodeRepository.deleteAllInBatch();
    oauthRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  @Test
  @DisplayName("동일한 SSO identity의 동시 최초 로그인은 내부 사용자 하나로 수렴한다")
  void upsert_whenFirstLoginIsConcurrent_createsOneUser() throws Exception {
    OAuthLoginProfile profile =
        new OAuthLoginProfile(
            OAuthProvider.GITHUB,
            "concurrent-provider-user",
            "concurrent@example.com",
            true,
            true,
            "concurrent-user",
            "Concurrent User",
            null);
    CyclicBarrier barrier = new CyclicBarrier(2);

    Future<UserAccount> first =
        executor.submit(
            () -> {
              barrier.await();
              return ssoAccountService.upsert(profile, NOW);
            });
    Future<UserAccount> second =
        executor.submit(
            () -> {
              barrier.await();
              return ssoAccountService.upsert(profile, NOW);
            });

    UserAccount firstUser = first.get();
    UserAccount secondUser = second.get();

    assertThat(firstUser.getId()).isEqualTo(secondUser.getId());
    assertThat(userRepository.count()).isEqualTo(1);
    assertThat(oauthRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("동일한 신뢰 이메일의 서로 다른 provider 동시 로그인은 내부 사용자 하나에 연결된다")
  void upsert_whenDifferentProvidersWithSameAuthoritativeEmailAreConcurrent_linksOneUser()
      throws Exception {
    OAuthLoginProfile githubProfile =
        new OAuthLoginProfile(
            OAuthProvider.GITHUB,
            "concurrent-github-user",
            "same@gmail.com",
            true,
            true,
            "github-user",
            "GitHub User",
            null);
    OAuthLoginProfile googleProfile =
        new OAuthLoginProfile(
            OAuthProvider.GOOGLE,
            "concurrent-google-user",
            "Same@Gmail.COM",
            true,
            true,
            "google-user",
            "Google User",
            null);
    CyclicBarrier barrier = new CyclicBarrier(2);

    Future<UserAccount> github =
        executor.submit(
            () -> {
              barrier.await();
              return ssoAccountService.upsert(githubProfile, NOW);
            });
    Future<UserAccount> google =
        executor.submit(
            () -> {
              barrier.await();
              return ssoAccountService.upsert(googleProfile, NOW);
            });

    UserAccount githubUser = github.get();
    UserAccount googleUser = google.get();

    assertThat(githubUser.getId()).isEqualTo(googleUser.getId());
    assertThat(githubUser.getPrimaryEmail()).isEqualTo("same@gmail.com");
    assertThat(userRepository.count()).isEqualTo(1);
    assertThat(oauthRepository.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("동일한 refresh token의 동시 rotation은 한 요청만 성공하고 family를 유지한다")
  void rotate_whenRequestsAreConcurrent_rotatesExactlyOnceWithinGrace() throws Exception {
    UserAccount user =
        userRepository.saveAndFlush(
            UserAccount.create("Concurrent User", "concurrent@example.com", null, NOW));
    Sha256Hash currentHash = hash('a');
    LoginRefreshSessionIssue issue =
        refreshTokenService.createSession(
            user.getId(), currentHash, NOW, REFRESH_TTL, ABSOLUTE_TTL);
    Instant rotatedAt = NOW.plusSeconds(1);
    CyclicBarrier barrier = new CyclicBarrier(2);

    Future<LoginRefreshRotationResult> first =
        executor.submit(
            () -> {
              barrier.await();
              return refreshTokenService.rotate(
                  currentHash, hash('b'), rotatedAt, REFRESH_TTL, GRACE);
            });
    Future<LoginRefreshRotationResult> second =
        executor.submit(
            () -> {
              barrier.await();
              return refreshTokenService.rotate(
                  currentHash, hash('c'), rotatedAt, REFRESH_TTL, GRACE);
            });

    List<LoginRefreshRotationStatus> statuses =
        List.of(first.get().status(), second.get().status());

    assertThat(statuses)
        .containsExactlyInAnyOrder(
            LoginRefreshRotationStatus.ROTATED, LoginRefreshRotationStatus.REUSED_WITHIN_GRACE);
    assertThat(sessionRepository.findById(issue.sessionId()).orElseThrow().isRevoked()).isFalse();
    assertThat(tokenRepository.count()).isEqualTo(2);

    LoginRefreshRotationResult replay =
        refreshTokenService.rotate(
            currentHash, hash('d'), rotatedAt.plus(GRACE).plusNanos(1), REFRESH_TTL, GRACE);

    assertThat(replay.status())
        .isEqualTo(LoginRefreshRotationStatus.REUSE_DETECTED_SESSION_REVOKED);
    assertThat(sessionRepository.findById(issue.sessionId()).orElseThrow().getRevokeReason())
        .isEqualTo(RefreshSessionRevocationReason.TOKEN_REUSE_DETECTED);
  }

  @Test
  @DisplayName("동일한 로그인 교환 코드의 동시 소비는 한 요청만 성공한다")
  void consume_whenExchangeRequestsAreConcurrent_consumesExactlyOnce() throws Exception {
    UserAccount user =
        userRepository.saveAndFlush(
            UserAccount.create("Concurrent User", "concurrent@example.com", null, NOW));
    Sha256Hash codeHash = hash('e');
    exchangeCodeService.issue(user.getId(), codeHash, NOW, Duration.ofSeconds(60));
    CyclicBarrier barrier = new CyclicBarrier(2);

    Future<LoginExchangeCodeConsumeResult> first =
        executor.submit(
            () -> {
              barrier.await();
              return exchangeCodeService.consume(codeHash, NOW.plusSeconds(1));
            });
    Future<LoginExchangeCodeConsumeResult> second =
        executor.submit(
            () -> {
              barrier.await();
              return exchangeCodeService.consume(codeHash, NOW.plusSeconds(1));
            });

    assertThat(List.of(first.get().status(), second.get().status()))
        .containsExactlyInAnyOrder(
            LoginExchangeCodeConsumeStatus.CONSUMED,
            LoginExchangeCodeConsumeStatus.ALREADY_CONSUMED);
  }

  private static Sha256Hash hash(char value) {
    return new Sha256Hash(String.valueOf(value).repeat(64));
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EntityScan(basePackages = "app.domain")
  @EnableJpaRepositories(basePackages = "app.storage.rds")
  @Import({
    SsoAccountService.class,
    SsoAccountTransactionService.class,
    LoginExchangeCodeService.class,
    LoginRefreshTokenService.class
  })
  static class TestApplication {}
}
