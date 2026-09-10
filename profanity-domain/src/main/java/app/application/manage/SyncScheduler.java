package app.application.manage;

import app.application.filter.AhocorasickFilter;
import app.application.filter.ProfanityDictionaryReloadedEvent;
import app.core.data.elapsed.Elapsed;
import app.core.data.elapsed.ElapsedStartAt;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 사전을 주기적으로 재동기화합니다.
 *
 * <p>단어 개수만 비교하면 개수가 그대로인 변경(표현 수정, 사용 여부 토글)과 동시 수정을 놓치므로, 변경 여부를 추측하지 않고 매번 사전을 다시 읽습니다. 실제로 적재
 * 단어가 바뀐 경우에만 결과 캐시 무효화 이벤트를 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {
  private final AhocorasickFilter syncFilter;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock loginAuthClock;

  /*
  추후 서버간 중복 스케줄러 방지를 위한 어노테이션
  @SchedulerLock(name = "synchronizeProfanityData",lockAtLeastFor = "PT10S",     lockAtMostFor = "PT1M")
  */
  @Scheduled(fixedDelay = 60000)
  public void synchronizeProfanityData() {
    ElapsedStartAt start = ElapsedStartAt.now();
    boolean changed = syncFilter.synchronizeProfanityTrie();
    Elapsed elapsed = Elapsed.end(start);
    if (!changed) {
      log.debug("스케줄러 동기화 완료 - 사전 변경 없음 [소요시간: {}ms]", elapsed);
      return;
    }
    log.info("비속어 데이터 변경 감지 - 동기화 완료 [소요시간: {}ms]", elapsed);
    eventPublisher.publishEvent(new ProfanityDictionaryReloadedEvent(loginAuthClock.instant()));
  }
}
