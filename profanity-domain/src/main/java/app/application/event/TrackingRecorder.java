package app.application.event;

import app.domain.record.RecordRepository;
import app.domain.record.Records;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class TrackingRecorder {

    private static final Logger log = LogManager.getLogger(TrackingRecorder.class);
    private final RecordRepository recordRepository;

    public TrackingRecorder(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @Transactional
    public void recordTracking(FilterEvent event) {
        log.info("[DOMAIN] record tracking : {}", event);
        String words = event.words().stream()
                .map(String::trim)
                .collect(Collectors.joining("/"));

        Records records = new Records.Builder()
                .trackingId(event.trackingId())
                .mode(event.mode())
                .apiKey(event.apiKey())
                .requestText(event.requestText())
                .words(words)
                .referrer(event.referrer())
                .ip(event.ip())
                .build();

        Records save = recordRepository.save(records);
        log.info("[DOMAIN] 레코드 데이터베이스 저장 완료  : {}", save);
    }
}
