package app.application.auth;

import java.util.UUID;

public record LoginExchangeCodeConsumeResult(
    LoginExchangeCodeConsumeStatus status, UUID codeId, UUID userId) {

  public static LoginExchangeCodeConsumeResult consumed(UUID codeId, UUID userId) {
    return new LoginExchangeCodeConsumeResult(
        LoginExchangeCodeConsumeStatus.CONSUMED, codeId, userId);
  }

  public static LoginExchangeCodeConsumeResult failed(
      LoginExchangeCodeConsumeStatus status, UUID codeId) {
    if (status == LoginExchangeCodeConsumeStatus.CONSUMED) {
      throw new IllegalArgumentException("Use consumed factory for a successful result");
    }
    return new LoginExchangeCodeConsumeResult(status, codeId, null);
  }

  public boolean isConsumed() {
    return status == LoginExchangeCodeConsumeStatus.CONSUMED;
  }
}
