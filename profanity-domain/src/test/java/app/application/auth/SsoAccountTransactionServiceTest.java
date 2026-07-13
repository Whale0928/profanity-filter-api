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
  @DisplayName("검증된 이메일이 같으면 대소문자와 공백을 정규해 기존 사용자에 OAuth 계정을 연결한다")
  void upsertInNewTransaction_whenVerifiedEmailMatches_linksExistingUser() {
    UserAccount githubUser =
        service.upsertInNewTransaction(
            profile(OAuthProvider.GITHUB, "github-user", " Same@Example.COM ", "GitHub User"),
            FIRST_LOGIN_AT);
    UserAccount googleUser =
        service.upsertInNewTransaction(
            profile(OAuthProvider.GOOGLE, "google-user", "same@example.com", "Google User"),
            FIRST_LOGIN_AT.plusSeconds(1));

    assertThat(googleUser.getId()).isEqualTo(githubUser.getId());
    assertThat(googleUser.getPrimaryEmail()).isEqualTo("same@example.com");
    assertThat(userRepository.size()).isEqualTo(1);
    assertThat(oauthRepository.size()).isEqualTo(2);
  }

  @Test
  @DisplayName("provider가 검증하지 않은 이메일이면 SSO 계정 생성을 거부한다")
  void upsertInNewTransaction_whenEmailIsUnverified_rejectsAccount() {
    OAuthLoginProfile unverifiedProfile =
        new OAuthLoginProfile(
            OAuthProvider.GITHUB,
            "unverified-user",
            "unverified@example.com",
            false,
            true,
            "unverified",
            "Unverified User",
            null);

    assertThatThrownBy(() -> service.upsertInNewTransaction(unverifiedProfile, FIRST_LOGIN_AT))
        .isInstanceOfSatisfying(
            LoginAccountUnavailableException.class,
            exception ->
                assertThat(exception.reason())
                    .isEqualTo(LoginAccountUnavailableException.Reason.VERIFIED_EMAIL_REQUIRED));
    assertThat(userRepository.size()).isZero();
    assertThat(oauthRepository.size()).isZero();
  }

  @Test
  @DisplayName("미검증 이메일 재로그인은 거부하고 기존 사용자를 변경하지 않는다")
  void upsertInNewTransaction_whenReloginEmailIsUnverified_rejectsWithoutChanges() {
    OAuthLoginProfile verifiedProfile =
        profile(OAuthProvider.GITHUB, "provider-user-2", "verified@example.com", "Verified");
    UserAccount first = service.upsertInNewTransaction(verifiedProfile, FIRST_LOGIN_AT);
    OAuthLoginProfile unverifiedProfile =
        new OAuthLoginProfile(
            OAuthProvider.GITHUB,
            "provider-user-2",
            "unverified@example.com",
            false,
            true,
            "unverified",
            "Unverified",
            null);

    assertThatThrownBy(
            () -> service.upsertInNewTransaction(unverifiedProfile, FIRST_LOGIN_AT.plusSeconds(30)))
        .isInstanceOf(LoginAccountUnavailableException.class);
    assertThat(first.getPrimaryEmail()).isEqualTo("verified@example.com");
  }

  @Test
  @DisplayName("기존 provider identity도 권위 있는 이메일이 아니면 upsert를 거부한다")
  void upsertInNewTransaction_whenIdentityExistsButEmailIsNotAuthoritative_rejectsUpsert() {
    OAuthLoginProfile trustedProfile =
        profile(OAuthProvider.GITHUB, "existing-identity", "existing@example.com", "Existing");
    UserAccount first = service.upsertInNewTransaction(trustedProfile, FIRST_LOGIN_AT);
    OAuthLoginProfile untrustedProfile =
        new OAuthLoginProfile(
            OAuthProvider.GITHUB,
            "existing-identity",
            "existing@example.com",
            true,
            false,
            "existing",
            "Existing Updated",
            null);

    assertThatThrownBy(
            () -> service.upsertInNewTransaction(untrustedProfile, FIRST_LOGIN_AT.plusSeconds(30)))
        .isInstanceOfSatisfying(
            LoginAccountUnavailableException.class,
            exception ->
                assertThat(exception.reason())
                    .isEqualTo(
                        LoginAccountUnavailableException.Reason.AUTHORITATIVE_EMAIL_REQUIRED));
    assertThat(first.getDisplayName()).isEqualTo("Existing");
    assertThat(userRepository.size()).isEqualTo(1);
    assertThat(oauthRepository.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("신뢰할 수 없는 이메일은 기존 사용자에 새 OAuth 계정으로 연결하지 않는다")
  void upsertInNewTransaction_whenEmailMatchesButIsUntrusted_rejectsLink() {
    UserAccount existing =
        service.upsertInNewTransaction(
            profile(OAuthProvider.GITHUB, "trusted-user", "shared@example.com", "Trusted"),
            FIRST_LOGIN_AT);
    OAuthLoginProfile untrustedProfile =
        new OAuthLoginProfile(
            OAuthProvider.GOOGLE,
            "untrusted-user",
            " SHARED@EXAMPLE.COM ",
            true,
            false,
            "untrusted",
            "Untrusted",
            null);

    assertThatThrownBy(
            () -> service.upsertInNewTransaction(untrustedProfile, FIRST_LOGIN_AT.plusSeconds(1)))
        .isInstanceOfSatisfying(
            LoginAccountUnavailableException.class,
            exception ->
                assertThat(exception.reason())
                    .isEqualTo(
                        LoginAccountUnavailableException.Reason.AUTHORITATIVE_EMAIL_REQUIRED));
    assertThat(existing.getDisplayName()).isEqualTo("Trusted");
    assertThat(userRepository.size()).isEqualTo(1);
    assertThat(oauthRepository.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("동일 사용자가 없어도 권위 있는 이메일이 아니면 신규 계정을 생성하지 않는다")
  void upsertInNewTransaction_whenEmailIsNotAuthoritativeAndUnused_rejectsCreation() {
    OAuthLoginProfile untrustedProfile =
        new OAuthLoginProfile(
            OAuthProvider.GITHUB,
            "new-untrusted-user",
            "NEW@EXAMPLE.COM",
            true,
            false,
            "new-untrusted",
            "New Untrusted",
            null);

    assertThatThrownBy(() -> service.upsertInNewTransaction(untrustedProfile, FIRST_LOGIN_AT))
        .isInstanceOfSatisfying(
            LoginAccountUnavailableException.class,
            exception ->
                assertThat(exception.reason())
                    .isEqualTo(
                        LoginAccountUnavailableException.Reason.AUTHORITATIVE_EMAIL_REQUIRED));
    assertThat(userRepository.size()).isZero();
    assertThat(oauthRepository.size()).isZero();
  }

  @Test
  @DisplayName("provider에 검증된 이메일 값이 없으면 SSO 계정 생성을 거부한다")
  void upsertInNewTransaction_whenVerifiedEmailIsMissing_rejectsAccount() {
    OAuthLoginProfile missingEmailProfile =
        new OAuthLoginProfile(
            OAuthProvider.GOOGLE, "missing-email", null, true, true, null, "Missing Email", null);

    assertThatThrownBy(() -> service.upsertInNewTransaction(missingEmailProfile, FIRST_LOGIN_AT))
        .isInstanceOfSatisfying(
            LoginAccountUnavailableException.class,
            exception ->
                assertThat(exception.reason())
                    .isEqualTo(LoginAccountUnavailableException.Reason.VERIFIED_EMAIL_REQUIRED));
  }

  private static OAuthLoginProfile profile(
      OAuthProvider provider, String providerUserId, String email, String displayName) {
    return new OAuthLoginProfile(
        provider, providerUserId, email, true, true, displayName, displayName, null);
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
