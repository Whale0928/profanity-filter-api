package app.application.manage;

import app.core.data.Const;
import app.core.data.Pair;
import app.domain.client.ClientsRepository;
import app.domain.client.Report;
import app.domain.client.ReportRepository;
import app.domain.record.RecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultReportManagementService implements ReportManagementService {

    private final ReportRepository reportRepository;
    private final ClientsRepository clientsRepository;
    private final RecordRepository recordRepository;

    @Override
    @Transactional
    public int createDailyReport() {
        List<Report> reports = new ArrayList<>();
        LocalDateTime yesterday = Const.getCurrentDateTime().minusDays(1);

        clientsRepository.
                findAll().
                parallelStream().
                forEach(
                        client -> {
                            Report todayReport = Report.createTodayReport(client);
                            Pair<Long, Long> report = recordRepository.getClientDailyUsageStatistics(client.getApiKey(), yesterday);
                            todayReport.updateCounts(report.getFirst(), report.getSecond());
                            reports.add(todayReport);
                        }
                );
        return reportRepository.saveAll(reports).size();
    }
}
