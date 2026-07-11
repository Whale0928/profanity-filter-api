package app.application.auth;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import app.domain.user.OAuthAccount;
import app.domain.user.OAuthAccountRepository;
import app.domain.user.OAuthLoginProfile;
import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import java.time.Instant;
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
    return synchronizeExisting(profile, now).orElseGet(() -> createAccount(profile, now));
  }

  @Transactional(propagation = REQUIRES_NEW)
  public Optional<UserAccount> synchronizeExistingInNewTransaction(
      OAuthLoginProfile profile, Instant now) {
    return synchronizeExisting(profile, now);
  }

  private Optional<UserAccount> synchronizeExisting(OAuthLoginProfile profile, Instant now) {
    return oauthAccountRepository
        .findByProviderAndProviderUserIdForUpdate(profile.provider(), profile.providerUserId())
        .map(
            oauthAccount -> {
              UserAccount userAccount = findRequiredUser(oauthAccount);
              userAccount.synchronizeProfile(
                  profile.displayName(), verifiedPrimaryEmail(profile), profile.avatarUrl(), now);
              oauthAccount.synchronizeProfile(profile);
              userAccountRepository.save(userAccount);
              oauthAccountRepository.save(oauthAccount);
              return userAccount;
            });
  }

  private UserAccount createAccount(OAuthLoginProfile profile, Instant now) {
    UserAccount userAccount =
        UserAccount.create(
            profile.displayName(), verifiedPrimaryEmail(profile), profile.avatarUrl(), now);
    userAccountRepository.save(userAccount);
    oauthAccountRepository.save(OAuthAccount.link(userAccount.getId(), profile, now));
    return userAccount;
  }

  private UserAccount findRequiredUser(OAuthAccount oauthAccount) {
    return userAccountRepository
        .findById(oauthAccount.getUserId())
        .orElseThrow(() -> new IllegalStateException("OAuth account references a missing user"));
  }

  private static String verifiedPrimaryEmail(OAuthLoginProfile profile) {
    return profile.emailVerified() ? profile.providerEmail() : null;
  }
}
