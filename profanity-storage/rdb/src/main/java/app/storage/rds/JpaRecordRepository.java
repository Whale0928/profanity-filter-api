package app.storage.rds;

import app.core.data.Pair;
import app.domain.record.RecordRepository;
import app.domain.record.Records;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface JpaRecordRepository extends RecordRepository, JpaRepository<Records, Long> {

    @Override
    @Query("""
            SELECT new app.core.data.Pair(COUNT(r.id),COUNT(case when length( r.words) > 0 then 1 else null end))
            FROM records r
            WHERE r.apiKey = :apiKey
            AND r.createdAt >= :yesterday
            """)
    Pair<Long, Long> getClientDailyUsageStatistics(String apiKey, LocalDateTime yesterday);
}
