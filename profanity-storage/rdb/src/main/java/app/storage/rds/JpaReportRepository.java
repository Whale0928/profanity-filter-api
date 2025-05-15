package app.storage.rds;

import app.domain.client.Report;
import app.domain.client.ReportRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaReportRepository extends ReportRepository, JpaRepository<Report, UUID> {
}
