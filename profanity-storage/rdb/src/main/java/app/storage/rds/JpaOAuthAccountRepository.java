package app.storage.rds;

import app.domain.user.OAuthAccount;
import app.domain.user.OAuthAccountRepository;
import app.domain.user.OAuthProvider;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaOAuthAccountRepository
    extends OAuthAccountRepository, JpaRepository<OAuthAccount, UUID> {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select a from oauth_accounts a
      where a.provider = :provider and a.providerUserId = :providerUserId
      """)
  Optional<OAuthAccount> findByProviderAndProviderUserIdForUpdate(
      @Param("provider") OAuthProvider provider, @Param("providerUserId") String providerUserId);
}
