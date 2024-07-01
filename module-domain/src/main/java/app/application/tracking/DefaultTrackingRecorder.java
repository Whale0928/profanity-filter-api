package app.application.tracking;

import app.application.event.FilterEvent;
import app.domain.record.RecordRepository;
import app.domain.record.Records;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DefaultTrackingRecorder implements TrackingRecorder {

    private static final Logger log = LogManager.getLogger(DefaultTrackingRecorder.class);
    private final RecordRepository recordRepository;
    private final List<String> localHostPatterns = Arrays.asList("127.0.0.1", "::1", "localhost", "localhost:9999");
    private final AntPathMatcher matcher = new AntPathMatcher();

    public DefaultTrackingRecorder(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @Override
    @Transactional
    public void recordTracking(FilterEvent event) {
        log.info("[DOMAIN] record tracking : {}", event);
        String ip = event.ip();

        boolean isLocalHost = localHostPatterns.stream().anyMatch(pattern -> matcher.match(pattern, ip));
        if (isLocalHost) {
            log.info("[DOMAIN] 로컬 호스트 IP로 인해 레코드 저장을 건너뜁니다 : {}", ip);
            return;
        }

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
        log.info("[DOMAIN] 레코드 데이터베이스 저장 완료  : {}", save.toString());
    }
}
