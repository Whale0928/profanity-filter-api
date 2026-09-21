package app.domain;

import app.core.data.Pair;
import app.domain.record.ApiKeyRequestVolume;
import app.domain.record.DailyRequestVolume;
import app.domain.record.ModeRequestVolume;
import app.domain.record.RecordRepository;
import app.domain.record.Records;
import app.domain.record.RequestVolume;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 요청 기록 저장소의 테스트 더블입니다.
 *
 * <p>Records는 생성 시각을 @CreationTimestamp로 채워 테스트에서 시각을 지정할 수 없으므로, 집계 결과를 직접 넣어 두고 돌려줍니다. 질의 자체의
 * 정확성은 실제 MySQL을 쓰는 통합 테스트에서 확인하고, 여기서는 집계 결과를 화면용으로 조립하는 로직을 검증합니다.
 */
public class InmemoryRecordRepository implements RecordRepository {

  private final Map<Long, Records> repository = new HashMap<>();

  private RequestVolume requestVolume = RequestVolume.empty();
  private List<DailyRequestVolume> dailyVolumes = List.of();
  private List<ModeRequestVolume> modeVolumes = List.of();
  private List<ApiKeyRequestVolume> apiKeyVolumes = List.of();

  private LocalDateTime requestedFrom;
  private LocalDateTime requestedTo;
  private LocalDate requestedFromDate;
  private Integer requestedTopLimit;

  @Override
  public Optional<Records> findById(Long id) {
    return Optional.ofNullable(repository.get(id));
  }

  @Override
  public Records save(Records records) {
    return records;
  }

  @Override
  public void delete(Records records) {}

  @Override
  public Pair<Long, Long> getApiKeyDailyUsageStatistics(
      String apiKeyHash, LocalDateTime yesterday) {
    return null;
  }

  @Override
  public Optional<Records> findByTrackingId(UUID trackingId) {
    return Optional.empty();
  }

  @Override
  public RequestVolume countRequestVolume(LocalDateTime from, LocalDateTime to) {
    rememberRange(from, to);
    return requestVolume;
  }

  @Override
  public List<DailyRequestVolume> aggregateDailyVolume(
      LocalDateTime from, LocalDateTime to, LocalDate fromDate) {
    rememberRange(from, to);
    this.requestedFromDate = fromDate;
    return dailyVolumes;
  }

  @Override
  public List<ModeRequestVolume> aggregateModeVolume(LocalDateTime from, LocalDateTime to) {
    rememberRange(from, to);
    return modeVolumes;
  }

  @Override
  public List<ApiKeyRequestVolume> aggregateTopApiKeys(
      LocalDateTime from, LocalDateTime to, int limit) {
    rememberRange(from, to);
    this.requestedTopLimit = limit;
    return apiKeyVolumes;
  }

  private void rememberRange(LocalDateTime from, LocalDateTime to) {
    this.requestedFrom = from;
    this.requestedTo = to;
  }

  public void setRequestVolume(RequestVolume requestVolume) {
    this.requestVolume = requestVolume;
  }

  public void setDailyVolumes(List<DailyRequestVolume> dailyVolumes) {
    this.dailyVolumes = List.copyOf(dailyVolumes);
  }

  public void setModeVolumes(List<ModeRequestVolume> modeVolumes) {
    this.modeVolumes = List.copyOf(modeVolumes);
  }

  public void setApiKeyVolumes(List<ApiKeyRequestVolume> apiKeyVolumes) {
    this.apiKeyVolumes = List.copyOf(apiKeyVolumes);
  }

  public LocalDateTime requestedFrom() {
    return requestedFrom;
  }

  public LocalDateTime requestedTo() {
    return requestedTo;
  }

  public LocalDate requestedFromDate() {
    return requestedFromDate;
  }

  public Integer requestedTopLimit() {
    return requestedTopLimit;
  }
}
