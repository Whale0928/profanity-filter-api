package app.application.auth;

import app.domain.auth.LoginExchangeCode;
import app.domain.auth.LoginExchangeCodeRepository;
import app.domain.auth.Sha256Hash;
import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginExchangeCodeService {

  private final LoginExchangeCodeRepository exchangeCodeRepository;
  private final UserAccountRepository userAccountRepository;

  @Transactional
  public LoginExchangeCodeIssue issue(
      UUID userId, Sha256Hash codeHash, Instant now, Duration timeToLive) {
    requireActiveUser(userId);
    LoginExchangeCode exchangeCode =
        exchangeCodeRepository.save(LoginExchangeCode.issue(userId, codeHash, now, timeToLive));
    return new LoginExchangeCodeIssue(
        exchangeCode.getId(), exchangeCode.getUserId(), exchangeCode.getExpiresAt());
  }

  @Transactional
  public LoginExchangeCodeConsumeResult consume(Sha256Hash codeHash, Instant now) {
    return exchangeCodeRepository
        .findByCodeHashForUpdate(codeHash.value())
        .map(exchangeCode -> consumeLocked(exchangeCode, now))
        .orElseGet(
            () ->
                LoginExchangeCodeConsumeResult.failed(
                    LoginExchangeCodeConsumeStatus.INVALID, null));
  }

  private LoginExchangeCodeConsumeResult consumeLocked(
      LoginExchangeCode exchangeCode, Instant now) {
    if (exchangeCode.isConsumed()) {
      return LoginExchangeCodeConsumeResult.failed(
          LoginExchangeCodeConsumeStatus.ALREADY_CONSUMED, exchangeCode.getId());
    }
    if (exchangeCode.isExpired(now)) {
      return LoginExchangeCodeConsumeResult.failed(
          LoginExchangeCodeConsumeStatus.EXPIRED, exchangeCode.getId());
    }

    UserAccount userAccount = findRequiredUser(exchangeCode.getUserId());
    exchangeCode.consume(now);
    exchangeCodeRepository.save(exchangeCode);
    if (!userAccount.isActive()) {
      return LoginExchangeCodeConsumeResult.failed(
          LoginExchangeCodeConsumeStatus.USER_INACTIVE, exchangeCode.getId());
    }
    return LoginExchangeCodeConsumeResult.consumed(exchangeCode.getId(), exchangeCode.getUserId());
  }

  private void requireActiveUser(UUID userId) {
    UserAccount userAccount = findRequiredUser(userId);
    if (!userAccount.isActive()) {
      throw new LoginAccountUnavailableException(
          LoginAccountUnavailableException.Reason.USER_INACTIVE);
    }
  }

  private UserAccount findRequiredUser(UUID userId) {
    return userAccountRepository
        .findByIdForUpdate(userId)
        .orElseThrow(
            () ->
                new LoginAccountUnavailableException(
                    LoginAccountUnavailableException.Reason.USER_NOT_FOUND));
  }
}
