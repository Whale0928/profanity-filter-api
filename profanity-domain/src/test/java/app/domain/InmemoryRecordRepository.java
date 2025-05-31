package app.domain;

import app.core.data.Pair;
import app.domain.record.RecordRepository;
import app.domain.record.Records;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InmemoryRecordRepository implements RecordRepository {

    private final Map<Long, Records> repository = new HashMap<>();

    @Override
    public Optional<Records> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public Records save(Records records) {
        return null;
    }

    @Override
    public void delete(Records records) {

    }

    @Override
    public Pair<Long, Long> getClientDailyUsageStatistics(String apiKey, LocalDateTime yesterday) {
        return null;
    }

    @Override
    public Optional<Records> findByTrackingId(UUID trackingId) {
        return Optional.empty();
    }
}
