package app.application.manage;

import app.domain.apikey.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기존 request_count 및 client_reports 집계 스케줄러입니다.
 *
 * @deprecated 사용량 수집 중단을 검토 중입니다. 제거 전까지 기존 집계만 유지합니다.
 */
@Deprecated(forRemoval = true)
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReportScheduler {
  private final ApiKeyRepository apiKeyRepository;
  private final ReportManagementService reportManagementService;

  /** API Key별 누적 요청 횟수를 갱신하는 기존 집계 스케줄러입니다. */
  @Transactional
  @Scheduled(cron = "0 0 1 * * ?")
  @SchedulerLock(name = "api_key_request_count_scheduler", lockAtMostFor = "PT5H")
  @Deprecated(forRemoval = true)
  public void updateRequestCount() {
    long start = System.currentTimeMillis();
    apiKeyRepository.updateRequestCount();
    long end = System.currentTimeMillis();
    log.info("API Key 요청 횟수 업데이트 완료 [소요시간: {}ms]", (end - start));
  }

  /** API Key별 전일 사용량을 client_reports에 저장하는 기존 집계 스케줄러입니다. */
  @Scheduled(cron = "0 0 1 * * ?")
  @SchedulerLock(name = "api_key_daily_report_scheduler", lockAtMostFor = "PT11H")
  @Deprecated(forRemoval = true)
  public void createDailyReport() {
    long start = System.currentTimeMillis();
    int dailyReport = reportManagementService.createDailyReport();
    long end = System.currentTimeMillis();
    log.info("일일 보고서 생성 완료 [소요시간: {}ms] [생성된 리포트 수: {}]", (end - start), dailyReport);
  }
}
