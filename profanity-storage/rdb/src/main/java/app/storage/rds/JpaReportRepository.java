package app.storage.rds;

import app.domain.client.Report;
import app.domain.client.ReportRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaReportRepository extends ReportRepository, JpaRepository<Report, UUID> {}
