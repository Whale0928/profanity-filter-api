package app.domain.record;


import app.core.data.Pair;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RecordRepository {

    Optional<Records> findById(Long id);

    Records save(Records records);

    void delete(Records records);

    Pair<Long, Long> getClientDailyUsageStatistics(String apiKey, LocalDateTime yesterday);

    Optional<Records> findByTrackingId(UUID trackingId);
}
