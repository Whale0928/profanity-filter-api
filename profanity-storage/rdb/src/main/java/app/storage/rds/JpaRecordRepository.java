package app.storage.rds;

import app.domain.record.RecordRepository;
import app.domain.record.Records;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRecordRepository extends RecordRepository, JpaRepository<Records, Long> {
}
