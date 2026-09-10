package app.application.apikey;

import static org.assertj.core.api.Assertions.assertThat;

import app.domain.InMemoryApiKeyRepository;
import app.domain.apikey.ApiKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiKeyUsageRecorderTest {

  private static final Instant NOW = Instant.parse("2026-09-10T03:00:00Z");
  private static final Instant ISSUED_INSTANT = Instant.parse("2026-09-01T01:00:00Z");

  private final InMemoryApiKeyRepository apiKeyRepository = new InMemoryApiKeyRepository();

  @Test
  @DisplayName("한 번도 사용되지 않은 키는 즉시 사용 시각을 기록한다")
  void recordUsage_firstUse_records() {
    ApiKey apiKey = saveKey();
    ApiKeyUsageRecorder recorder = recorderAt(NOW);

    assertThat(recorder.recordUsage(apiKey.getId())).isTrue();
    assertThat(apiKey.getLastUsedAt()).isEqualTo(recorder.now());
  }

  @Test
  @DisplayName("기록 간격 안에서 다시 인증하면 갱신하지 않는다")
  void recordUsage_withinInterval_skipsWrite() {
    ApiKey apiKey = saveKey();
    recorderAt(NOW).recordUsage(apiKey.getId());
    var firstRecordedAt = apiKey.getLastUsedAt();

    boolean recorded =
        recorderAt(NOW.plus(ApiKeyUsageRecorder.RECORD_INTERVAL.minusSeconds(1)))
            .recordUsage(apiKey.getId());

    assertThat(recorded).isFalse();
    assertThat(apiKey.getLastUsedAt()).isEqualTo(firstRecordedAt);
  }

  @Test
  @DisplayName("기록 간격이 지나면 다시 갱신한다")
  void recordUsage_afterInterval_recordsAgain() {
    ApiKey apiKey = saveKey();
    recorderAt(NOW).recordUsage(apiKey.getId());
    var firstRecordedAt = apiKey.getLastUsedAt();

    ApiKeyUsageRecorder later = recorderAt(NOW.plus(ApiKeyUsageRecorder.RECORD_INTERVAL));

    assertThat(later.recordUsage(apiKey.getId())).isTrue();
    assertThat(apiKey.getLastUsedAt()).isAfter(firstRecordedAt);
  }

  @Test
  @DisplayName("없는 키는 아무것도 기록하지 않는다")
  void recordUsage_missingKey_doesNothing() {
    assertThat(recorderAt(NOW).recordUsage(UUID.randomUUID())).isFalse();
  }

  @Test
  @DisplayName("기록이 필요한지 판단하는 조건은 마지막 기록 시각과 간격으로 결정된다")
  void isUsageRecordStale_reflectsInterval() {
    ApiKey apiKey = saveKey();
    ApiKeyUsageRecorder recorder = recorderAt(NOW);

    assertThat(apiKey.isUsageRecordStale(recorder.now(), Duration.ofSeconds(60))).isTrue();
    recorder.recordUsage(apiKey.getId());
    assertThat(apiKey.isUsageRecordStale(recorder.now(), Duration.ofSeconds(60))).isFalse();
  }

  private ApiKeyUsageRecorder recorderAt(Instant instant) {
    return new ApiKeyUsageRecorder(apiKeyRepository, Clock.fixed(instant, ZoneOffset.UTC));
  }

  private ApiKey saveKey() {
    return apiKeyRepository.save(
        ApiKey.issue(
            UUID.randomUUID(),
            "사용 기록 대상",
            "usage@example.test",
            "hash-usage",
            "hint",
            "test",
            null,
            LocalDateTime.ofInstant(ISSUED_INSTANT, ZoneOffset.UTC)));
  }
}
