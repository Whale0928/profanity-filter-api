package app.application.admin;

import app.application.filter.AhocorasickFilter;
import app.application.filter.ProfanityDictionaryReloadedEvent;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 사전 변경이 커밋된 뒤 요청을 처리한 인스턴스의 Trie를 즉시 재동기화합니다.
 *
 * <p>커밋 이후에만 동작하므로 롤백된 변경이나 아직 보이지 않는 사전을 읽지 않습니다. 다른 인스턴스는 SyncScheduler가 최대 1분 안에 따라옵니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfanityDictionaryChangeListener {

  private final AhocorasickFilter ahocorasickFilter;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock loginAuthClock;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onDictionaryChanged(ProfanityDictionaryChangedEvent event) {
    log.info("[AdminWord] 사전 변경 커밋 확인 - Trie 재동기화 시작 wordId={}", event.wordId());
    if (ahocorasickFilter.synchronizeProfanityTrie()) {
      eventPublisher.publishEvent(new ProfanityDictionaryReloadedEvent(loginAuthClock.instant()));
    }
  }
}
