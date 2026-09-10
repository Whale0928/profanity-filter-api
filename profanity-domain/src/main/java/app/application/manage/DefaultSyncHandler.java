package app.application.manage;

import app.application.filter.AhocorasickFilter;
import app.application.filter.ProfanityDictionaryReloadedEvent;
import app.core.data.manage.response.ResultMessage;
import app.domain.manage.ManageAccountRepository;
import java.time.Clock;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class DefaultSyncHandler implements SyncHandler {

  private static final Logger log = LogManager.getLogger(DefaultSyncHandler.class);
  private final AhocorasickFilter ahocorasickFilter;
  private final ManageAccountRepository accountRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  public DefaultSyncHandler(
      AhocorasickFilter ahocorasickFilter,
      ManageAccountRepository manageAccountRepository,
      ApplicationEventPublisher eventPublisher,
      Clock clock) {
    this.ahocorasickFilter = ahocorasickFilter;
    this.accountRepository = manageAccountRepository;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Override
  public ResultMessage doSync(final String password) {
    log.info("단어 동기화 요청");
    Objects.requireNonNull(password, "비밀번호는 null일 수 없습니다.");

    accountRepository
        .findByPassword(password)
        .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 관리자입니다."));

    long start = System.nanoTime();
    if (ahocorasickFilter.synchronizeProfanityTrie()) {
      eventPublisher.publishEvent(new ProfanityDictionaryReloadedEvent(clock.instant()));
    }
    log.info("동기화 완료. 소요시간 : {}ms", (System.nanoTime() - start) / 1000000);

    return ResultMessage.SUCCESS_SYNC_WORD;
  }
}
