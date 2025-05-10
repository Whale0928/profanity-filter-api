package app.application.manage;


import app.domain.client.ClientsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일일 보고서 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReportScheduler {
    private final ClientsRepository clientsRepository;

    /**
     * 클라이언트 요청 횟수 업데이트 스케줄러
     */
    @Transactional
    @Scheduled(cron = "0 0 0/6 * * ?")
    @SchedulerLock(name = "daily_report_scheduler", lockAtMostFor = "PT5H")
    public void updateReportSchedule() {
        long start = System.currentTimeMillis();
        clientsRepository.updateClientRequestCount();
        long end = System.currentTimeMillis();
        log.info("클라이언트 요청 횟수 업데이트 완료 [소요시간: {}ms]", (end - start));
    }
}
