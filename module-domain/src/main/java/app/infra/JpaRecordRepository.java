package app.infra;

import app.domain.RecordRepository;
import app.domain.Records;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRecordRepository extends RecordRepository, JpaRepository<Records, Long> {
}
