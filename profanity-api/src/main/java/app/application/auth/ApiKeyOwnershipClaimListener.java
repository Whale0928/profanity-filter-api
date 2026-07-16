package app.application.auth;

import app.application.apikey.ApiKeyOwnershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyOwnershipClaimListener {
  private final ApiKeyOwnershipService apiKeyOwnershipService;

  @Async
  @EventListener
  public void claim(ApiKeyOwnershipClaimRequested event) {
    int claimed = apiKeyOwnershipService.claimUnownedKeys(event.userId(), event.verifiedEmail());
    if (claimed > 0) {
      log.info("기존 API Key 소유권 연결 완료 userId={} count={}", event.userId(), claimed);
    }
  }
}
