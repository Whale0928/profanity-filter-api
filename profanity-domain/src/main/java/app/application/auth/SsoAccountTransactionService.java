package app.application.auth;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import app.domain.user.OAuthAccount;
import app.domain.user.OAuthAccountRepository;
import app.domain.user.OAuthLoginProfile;
import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SsoAccountTransactionService {

  private final UserAccountRepository userAccountRepository;
  private final OAuthAccountRepository oauthAccountRepository;

  @Transactional(propagation = REQUIRES_NEW)
  public UserAccount upsertInNewTransaction(OAuthLoginProfile profile, Instant now) {
    String primaryEmail = verifiedPrimaryEmail(profile);
    requireAuthoritativeEmail(profile);
    return synchronizeExisting(profile, primaryEmail, now)
        .or(() -> linkExistingUser(profile, primaryEmail, now))
        .orElseGet(() -> createAccount(profile, primaryEmail, now));
  }

  @Transactional(propagation = REQUIRES_NEW)
  public Optional<UserAccount> synchronizeExistingInNewTransaction(
      OAuthLoginProfile profile, Instant now) {
    String primaryEmail = verifiedPrimaryEmail(profile);
    requireAuthoritativeEmail(profile);
    return synchronizeExisting(profile, primaryEmail, now)
        .or(() -> linkExistingUser(profile, primaryEmail, now));
  }

  private Optional<UserAccount> synchronizeExisting(
      OAuthLoginProfile profile, String primaryEmail, Instant now) {
    return oauthAccountRepository
        .findByProviderAndProviderUserIdForUpdate(profile.provider(), profile.providerUserId())
        .map(
            oauthAccount -> {
              UserAccount userAccount = findRequiredUser(oauthAccount);
              userAccount.synchronizeProfile(
                  profile.displayName(), primaryEmail, profile.avatarUrl(), now);
              oauthAccount.synchronizeProfile(profile);
              userAccountRepository.save(userAccount);
              oauthAccountRepository.save(oauthAccount);
              return userAccount;
            });
  }

  private Optional<UserAccount> linkExistingUser(
      OAuthLoginProfile profile, String primaryEmail, Instant now) {
    return userAccountRepository
        .findByPrimaryEmailForUpdate(primaryEmail)
        .map(
            userAccount -> {
              userAccount.synchronizeProfile(
                  profile.displayName(), primaryEmail, profile.avatarUrl(), now);
              userAccountRepository.save(userAccount);
              oauthAccountRepository.save(OAuthAccount.link(userAccount.getId(), profile, now));
              return userAccount;
            });
  }

  private UserAccount createAccount(OAuthLoginProfile profile, String primaryEmail, Instant now) {
    UserAccount userAccount =
        UserAccount.create(profile.displayName(), primaryEmail, profile.avatarUrl(), now);
    userAccountRepository.save(userAccount);
    oauthAccountRepository.save(OAuthAccount.link(userAccount.getId(), profile, now));
    return userAccount;
  }

  private UserAccount findRequiredUser(OAuthAccount oauthAccount) {
    return userAccountRepository
        .findByIdForUpdate(oauthAccount.getUserId())
        .orElseThrow(() -> new IllegalStateException("OAuth account references a missing user"));
  }

  private static String verifiedPrimaryEmail(OAuthLoginProfile profile) {
    if (!profile.emailVerified()
        || profile.providerEmail() == null
        || profile.providerEmail().isBlank()) {
      throw new LoginAccountUnavailableException(
          LoginAccountUnavailableException.Reason.VERIFIED_EMAIL_REQUIRED);
    }
    return profile.providerEmail().trim().toLowerCase(Locale.ROOT);
  }

  private static void requireAuthoritativeEmail(OAuthLoginProfile profile) {
    if (!profile.emailAuthoritative()) {
      throw new LoginAccountUnavailableException(
          LoginAccountUnavailableException.Reason.AUTHORITATIVE_EMAIL_REQUIRED);
    }
  }
}
