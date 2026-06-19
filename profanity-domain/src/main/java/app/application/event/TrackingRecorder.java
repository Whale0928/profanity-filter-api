package app.application.event;

import app.core.util.ApiKeys;
import app.domain.record.RecordRepository;
import app.domain.record.Records;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TrackingRecorder {

  private final RecordRepository recordRepository;

  public TrackingRecorder(RecordRepository recordRepository) {
    this.recordRepository = recordRepository;
  }

  @Transactional
  public void recordTracking(FilterEvent event) {
    String words = event.words().stream().map(String::trim).collect(Collectors.joining("/"));

    Records records =
        new Records.Builder()
            .trackingId(event.trackingId())
            .mode(event.mode())
            .apiKey(event.apiKey())
            .requestText(event.requestText())
            .words(words)
            .referrer(event.referrer())
            .ip(event.ip())
            .build();

    recordRepository.save(records);
    log.info(
        "[FILTER] 처리 결과 trackingId={} mode={} detected={} words=[{}] ip={} apiKey={}",
        event.trackingId(),
        event.mode(),
        event.words().size(),
        words,
        event.ip(),
        ApiKeys.mask(event.apiKey()));
  }
}
