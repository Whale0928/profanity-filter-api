package app.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.domain.user.OAuthAccount;
import app.domain.user.OAuthAccountRepository;
import app.domain.user.OAuthLoginProfile;
import app.domain.user.OAuthProvider;
import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class SsoAccountServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");

  @Test
  @DisplayName("동일 이메일 사용자의 동시 생성 충돌 시 한 번 재시도해 OAuth 계정을 연결한다")
  void upsert_whenSameEmailCreateConflicts_retriesAndLinksWinner() {
    ConflictOnceUserAccountRepository userRepository = new ConflictOnceUserAccountRepository();
    InMemoryOAuthAccountRepository oauthRepository = new InMemoryOAuthAccountRepository();
    SsoAccountTransactionService transactionService =
        new SsoAccountTransactionService(userRepository, oauthRepository);
    SsoAccountService service = new SsoAccountService(transactionService);
    OAuthLoginProfile profile =
        new OAuthLoginProfile(
            OAuthProvider.GITHUB,
            "concurrent-provider-user",
            " Race@Example.COM ",
            true,
            true,
            "race-user",
            "Race User",
            null);

    UserAccount result = service.upsert(profile, NOW);

    assertThat(result.getId()).isEqualTo(userRepository.winner().getId());
    assertThat(result.getPrimaryEmail()).isEqualTo("race@example.com");
    assertThat(userRepository.size()).isEqualTo(1);
    assertThat(userRepository.emailLookupCount()).isEqualTo(2);
    assertThat(oauthRepository.onlyAccount().getUserId()).isEqualTo(result.getId());
  }

  @Test
  @DisplayName("권위 있는 이메일이 아니면 race 처리 전에 SSO 계정 upsert를 거부한다")
  void upsert_whenEmailIsNotAuthoritative_rejectsBeforeRaceHandling() {
    ConflictOnceUserAccountRepository userRepository = new ConflictOnceUserAccountRepository();
    InMemoryOAuthAccountRepository oauthRepository = new InMemoryOAuthAccountRepository();
    SsoAccountTransactionService transactionService =
        new SsoAccountTransactionService(userRepository, oauthRepository);
    SsoAccountService service = new SsoAccountService(transactionService);
    OAuthLoginProfile profile =
        new OAuthLoginProfile(
            OAuthProvider.GITHUB,
            "untrusted-concurrent-user",
            "race@example.com",
            true,
            false,
            "untrusted-race",
            "Untrusted Race",
            null);

    assertThatThrownBy(() -> service.upsert(profile, NOW))
        .isInstanceOfSatisfying(
            LoginAccountUnavailableException.class,
            exception ->
                assertThat(exception.reason())
                    .isEqualTo(
                        LoginAccountUnavailableException.Reason.AUTHORITATIVE_EMAIL_REQUIRED));
    assertThat(userRepository.size()).isZero();
    assertThat(userRepository.emailLookupCount()).isZero();
    assertThat(oauthRepository.size()).isZero();
  }

  private static final class ConflictOnceUserAccountRepository implements UserAccountRepository {
    private final Map<UUID, UserAccount> values = new LinkedHashMap<>();
    private boolean conflictPending = true;
    private int emailLookupCount;
    private UserAccount winner;

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
      emailLookupCount++;
      return values.values().stream()
          .filter(user -> user.getPrimaryEmail().equalsIgnoreCase(primaryEmail.trim()))
          .findFirst();
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
      if (conflictPending) {
        conflictPending = false;
        winner =
            UserAccount.create(
                "Concurrent Winner", userAccount.getPrimaryEmail(), null, NOW.minusSeconds(1));
        values.put(winner.getId(), winner);
        throw new DataIntegrityViolationException("simulated unique email conflict");
      }
      values.put(userAccount.getId(), userAccount);
      return userAccount;
    }

    UserAccount winner() {
      return winner;
    }

    int size() {
      return values.size();
    }

    int emailLookupCount() {
      return emailLookupCount;
    }
  }

  private static final class InMemoryOAuthAccountRepository implements OAuthAccountRepository {
    private final Map<ProviderIdentity, OAuthAccount> values = new LinkedHashMap<>();

    @Override
    public Optional<OAuthAccount> findByProviderAndProviderUserIdForUpdate(
        OAuthProvider provider, String providerUserId) {
      return Optional.ofNullable(values.get(new ProviderIdentity(provider, providerUserId)));
    }

    @Override
    public OAuthAccount save(OAuthAccount oauthAccount) {
      values.put(
          new ProviderIdentity(oauthAccount.getProvider(), oauthAccount.getProviderUserId()),
          oauthAccount);
      return oauthAccount;
    }

    OAuthAccount onlyAccount() {
      return values.values().iterator().next();
    }

    int size() {
      return values.size();
    }
  }

  private record ProviderIdentity(OAuthProvider provider, String providerUserId) {}
}
