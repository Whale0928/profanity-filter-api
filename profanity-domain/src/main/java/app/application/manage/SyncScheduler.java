package app.application.manage;

import app.application.filter.AhocorasickFilter;
import app.core.data.elapsed.Elapsed;
import app.core.data.elapsed.ElapsedStartAt;
import app.domain.profanity.ProfanityRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {
    private final AhocorasickFilter syncFilter;
    private final ProfanityRepository profanityRepository;

    private long count;

    @PostConstruct
    public void postConstruct() {
        count = profanityRepository.countAll();
    }

    /*
    추후 서버간 중복 스케줄러 방지를 위한 어노테이션
    @SchedulerLock(name = "synchronizeProfanityData",lockAtLeastFor = "PT10S",     lockAtMostFor = "PT1M")
    */
    @Scheduled(fixedDelay = 60000)
    public void synchronizeProfanityData() {
        long counted = profanityRepository.countAll();
        if (counted != count) {
            log.info("비속어 데이터 변경 감지 - 동기화 시작 [DB: {} -> 서버: {}]", counted, count);
            ElapsedStartAt start = ElapsedStartAt.now();
            syncFilter.synchronizeProfanityTrie();
            Elapsed elapsed = Elapsed.end(start);
            log.info("스케줄러 동기화 작업이 완료되었습니다. [소요시간: {}ms]", elapsed);
            count = counted;
        }
    }
}
