package app.application.auth;

import static org.assertj.core.api.Assertions.assertThat;

import app.domain.auth.LoginExchangeCode;
import app.domain.auth.LoginExchangeCodeRepository;
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

class LoginExchangeCodeServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
  private static final Sha256Hash CODE_HASH = new Sha256Hash("a".repeat(64));

  private final InMemoryExchangeCodeRepository codeRepository =
      new InMemoryExchangeCodeRepository();
  private final InMemoryUserAccountRepository userRepository = new InMemoryUserAccountRepository();
  private final LoginExchangeCodeService service =
      new LoginExchangeCodeService(codeRepository, userRepository);
  private UserAccount user;

  @BeforeEach
  void setUp() {
    user = UserAccount.create("Tester", "tester@example.com", null, NOW);
    userRepository.save(user);
  }

  @Test
  @DisplayName("교환 코드는 유효 시간 안에 한 번만 소비할 수 있다")
  void consume_whenCodeIsValid_consumesOnlyOnce() {
    service.issue(user.getId(), CODE_HASH, NOW, Duration.ofSeconds(60));

    LoginExchangeCodeConsumeResult first = service.consume(CODE_HASH, NOW.plusSeconds(10));
    LoginExchangeCodeConsumeResult second = service.consume(CODE_HASH, NOW.plusSeconds(11));

    assertThat(first.status()).isEqualTo(LoginExchangeCodeConsumeStatus.CONSUMED);
    assertThat(first.userId()).isEqualTo(user.getId());
    assertThat(second.status()).isEqualTo(LoginExchangeCodeConsumeStatus.ALREADY_CONSUMED);
  }

  @Test
  @DisplayName("만료 시각이 지난 교환 코드는 소비하지 않는다")
  void consume_whenCodeExpired_returnsExpired() {
    service.issue(user.getId(), CODE_HASH, NOW, Duration.ofSeconds(60));

    LoginExchangeCodeConsumeResult result = service.consume(CODE_HASH, NOW.plusSeconds(60));

    assertThat(result.status()).isEqualTo(LoginExchangeCodeConsumeStatus.EXPIRED);
    assertThat(codeRepository.findByCodeHashForUpdate(CODE_HASH.value()).orElseThrow().isConsumed())
        .isFalse();
  }

  @Test
  @DisplayName("비활성 사용자의 교환 코드는 실패하더라도 재사용할 수 없게 소비한다")
  void consume_whenUserDisabled_consumesCodeAndRejectsLogin() {
    service.issue(user.getId(), CODE_HASH, NOW, Duration.ofSeconds(60));
    user.disable(NOW.plusSeconds(1));

    LoginExchangeCodeConsumeResult result = service.consume(CODE_HASH, NOW.plusSeconds(2));
    LoginExchangeCodeConsumeResult retried = service.consume(CODE_HASH, NOW.plusSeconds(3));

    assertThat(result.status()).isEqualTo(LoginExchangeCodeConsumeStatus.USER_INACTIVE);
    assertThat(retried.status()).isEqualTo(LoginExchangeCodeConsumeStatus.ALREADY_CONSUMED);
  }

  private static final class InMemoryExchangeCodeRepository implements LoginExchangeCodeRepository {
    private final Map<String, LoginExchangeCode> values = new LinkedHashMap<>();

    @Override
    public Optional<LoginExchangeCode> findByCodeHashForUpdate(String codeHash) {
      return Optional.ofNullable(values.get(codeHash));
    }

    @Override
    public LoginExchangeCode save(LoginExchangeCode exchangeCode) {
      values.put(exchangeCode.getCodeHash(), exchangeCode);
      return exchangeCode;
    }
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
}
