package app.application.manage;

import app.core.data.Const;
import app.core.data.Pair;
import app.domain.apikey.ApiKeyRepository;
import app.domain.client.Report;
import app.domain.client.ReportRepository;
import app.domain.record.RecordRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Deprecated(forRemoval = true)
public class DefaultReportManagementService implements ReportManagementService {

  private final ReportRepository reportRepository;
  private final ApiKeyRepository apiKeyRepository;
  private final RecordRepository recordRepository;

  @Override
  @Transactional
  public int createDailyReport() {
    List<Report> reports = new ArrayList<>();
    LocalDateTime yesterday = Const.getCurrentDateTime().minusDays(1);

    apiKeyRepository
        .findAll()
        .forEach(
            apiKey -> {
              Report todayReport = Report.createTodayReport(apiKey);
              Pair<Long, Long> report =
                  recordRepository.getApiKeyDailyUsageStatistics(apiKey.getKeyHash(), yesterday);
              todayReport.updateCounts(report.getFirst(), report.getSecond());
              reports.add(todayReport);
            });
    return reportRepository.saveAll(reports).size();
  }
}
