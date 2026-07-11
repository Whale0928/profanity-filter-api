package app.application.auth;

import static org.assertj.core.api.Assertions.assertThat;

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

class SsoAccountTransactionServiceTest {

  private static final Instant FIRST_LOGIN_AT = Instant.parse("2026-07-11T00:00:00Z");

  private final InMemoryUserAccountRepository userRepository = new InMemoryUserAccountRepository();
  private final InMemoryOAuthAccountRepository oauthRepository =
      new InMemoryOAuthAccountRepository();
  private final SsoAccountTransactionService service =
      new SsoAccountTransactionService(userRepository, oauthRepository);

  @Test
  @DisplayName("동일한 provider 사용자가 다시 로그인하면 기존 내부 사용자를 갱신한다")
  void upsertInNewTransaction_whenSameProviderUser_updatesExistingUser() {
    OAuthLoginProfile firstProfile =
        profile(OAuthProvider.GITHUB, "provider-user-1", "first@example.com", "First");
    UserAccount first = service.upsertInNewTransaction(firstProfile, FIRST_LOGIN_AT);

    OAuthLoginProfile changedProfile =
        profile(OAuthProvider.GITHUB, "provider-user-1", "changed@example.com", "Changed");
    UserAccount second =
        service.upsertInNewTransaction(changedProfile, FIRST_LOGIN_AT.plusSeconds(30));

    assertThat(second.getId()).isEqualTo(first.getId());
    assertThat(second.getDisplayName()).isEqualTo("Changed");
    assertThat(second.getPrimaryEmail()).isEqualTo("changed@example.com");
    assertThat(userRepository.size()).isEqualTo(1);
    assertThat(oauthRepository.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("이메일이 같아도 provider identity가 다르면 사용자를 자동 연결하지 않는다")
  void upsertInNewTransaction_whenOnlyEmailMatches_createsSeparateUsers() {
    UserAccount githubUser =
        service.upsertInNewTransaction(
            profile(OAuthProvider.GITHUB, "github-user", "same@example.com", "GitHub User"),
            FIRST_LOGIN_AT);
    UserAccount googleUser =
        service.upsertInNewTransaction(
            profile(OAuthProvider.GOOGLE, "google-user", "same@example.com", "Google User"),
            FIRST_LOGIN_AT.plusSeconds(1));

    assertThat(googleUser.getId()).isNotEqualTo(githubUser.getId());
    assertThat(userRepository.size()).isEqualTo(2);
    assertThat(oauthRepository.size()).isEqualTo(2);
  }

  @Test
  @DisplayName("provider가 검증하지 않은 이메일은 내부 대표 이메일로 설정하지 않는다")
  void upsertInNewTransaction_whenEmailIsUnverified_doesNotUseItAsPrimaryEmail() {
    OAuthLoginProfile unverifiedProfile =
        new OAuthLoginProfile(
            OAuthProvider.GITHUB,
            "unverified-user",
            "unverified@example.com",
            false,
            "unverified",
            "Unverified User",
            null);

    UserAccount user = service.upsertInNewTransaction(unverifiedProfile, FIRST_LOGIN_AT);

    assertThat(user.getPrimaryEmail()).isNull();
  }

  @Test
  @DisplayName("미검증 이메일 재로그인은 기존의 검증된 대표 이메일을 덮지 않는다")
  void upsertInNewTransaction_whenReloginEmailIsUnverified_keepsVerifiedPrimaryEmail() {
    OAuthLoginProfile verifiedProfile =
        profile(OAuthProvider.GITHUB, "provider-user-2", "verified@example.com", "Verified");
    UserAccount first = service.upsertInNewTransaction(verifiedProfile, FIRST_LOGIN_AT);
    OAuthLoginProfile unverifiedProfile =
        new OAuthLoginProfile(
            OAuthProvider.GITHUB,
            "provider-user-2",
            "unverified@example.com",
            false,
            "unverified",
            "Unverified",
            null);

    UserAccount second =
        service.upsertInNewTransaction(unverifiedProfile, FIRST_LOGIN_AT.plusSeconds(30));

    assertThat(second.getId()).isEqualTo(first.getId());
    assertThat(second.getPrimaryEmail()).isEqualTo("verified@example.com");
  }

  private static OAuthLoginProfile profile(
      OAuthProvider provider, String providerUserId, String email, String displayName) {
    return new OAuthLoginProfile(
        provider, providerUserId, email, true, displayName, displayName, null);
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
    public UserAccount save(UserAccount userAccount) {
      values.put(userAccount.getId(), userAccount);
      return userAccount;
    }

    int size() {
      return values.size();
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

    int size() {
      return values.size();
    }
  }

  private record ProviderIdentity(OAuthProvider provider, String providerUserId) {}
}
