package app.domain.user;

import java.util.Optional;

public interface OAuthAccountRepository {
  Optional<OAuthAccount> findByProviderAndProviderUserIdForUpdate(
      OAuthProvider provider, String providerUserId);

  OAuthAccount save(OAuthAccount oauthAccount);
}
