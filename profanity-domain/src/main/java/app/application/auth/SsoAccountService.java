package app.application.auth;

import app.domain.user.OAuthLoginProfile;
import app.domain.user.UserAccount;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SsoAccountService {

  private final SsoAccountTransactionService transactionService;

  public UserAccount upsert(OAuthLoginProfile profile, Instant now) {
    try {
      return transactionService.upsertInNewTransaction(profile, now);
    } catch (DataAccessException concurrentCreateConflict) {
      Optional<UserAccount> winner =
          transactionService.synchronizeExistingInNewTransaction(profile, now);
      if (winner.isPresent()) {
        return winner.get();
      }
      throw concurrentCreateConflict;
    }
  }
}
