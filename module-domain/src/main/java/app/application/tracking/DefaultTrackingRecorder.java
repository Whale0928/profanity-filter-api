package app.application.tracking;

import app.application.event.FilterEvent;
import app.domain.RecordRepository;
import app.domain.Records;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultTrackingRecorder implements TrackingRecorder {

    private static final Logger log = LogManager.getLogger(DefaultTrackingRecorder.class);

    private final RecordRepository recordRepository;

    public DefaultTrackingRecorder(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @Override
    @Transactional
    public void recordTracking(FilterEvent event) {
        log.info("[DOMAIN] record tracking : {}", event);

        Records records = new Records.Builder()
                .trackingId(event.trackingId())
                .apiKey(event.apiKey())
                .requestText(event.requestText())
                .words(event.words().stream().map(i -> i + "/").reduce("", String::concat))
                .referrer(event.referrer())
                .ip(event.ip())
                .build();

        Records save = recordRepository.save(records);
        log.info("[DOMAIN] 레코드 데이터베이스 저장 완료  : {}", save.toString());
    }
}
