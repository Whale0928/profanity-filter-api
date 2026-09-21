package app.domain.record;

import app.core.data.Pair;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecordRepository {

  Optional<Records> findById(Long id);

  Records save(Records records);

  void delete(Records records);

  Pair<Long, Long> getApiKeyDailyUsageStatistics(String apiKeyHash, LocalDateTime yesterday);

  Optional<Records> findByTrackingId(UUID trackingId);

  /**
   * 기간 내 총 요청 수와 검출된 요청 수를 셉니다.
   *
   * <p>경계는 records.created_at이 저장된 시간대의 벽시계 값으로 넘겨야 합니다. 이 컬럼은 JVM 기본 시간대로 기록되기 때문에 서비스 시간대와 다를 수
   * 있습니다.
   *
   * @param from 조회 시작 시각(포함)
   * @param to 조회 종료 시각(제외)
   */
  RequestVolume countRequestVolume(LocalDateTime from, LocalDateTime to);

  /**
   * 기간 내 요청을 하루 단위로 집계합니다. 요청이 없던 날은 결과에 포함되지 않습니다.
   *
   * <p>일자는 저장된 값의 시간대를 해석하지 않고 시작 경계로부터 며칠째인지로 나눕니다. created_at이 어떤 시간대의 벽시계로 저장되든 경계와 같은 기준이라 결과가
   * 어긋나지 않습니다.
   *
   * @param from 조회 시작 시각(포함)
   * @param to 조회 종료 시각(제외)
   * @param fromDate from 경계에 해당하는 서비스 시간대 일자. 집계 결과에 일자를 되돌려 붙일 때 사용합니다.
   */
  List<DailyRequestVolume> aggregateDailyVolume(
      LocalDateTime from, LocalDateTime to, LocalDate fromDate);

  /** 기간 내 요청을 필터링 모드별로 집계합니다. 요청이 없던 모드는 결과에 포함되지 않습니다. */
  List<ModeRequestVolume> aggregateModeVolume(LocalDateTime from, LocalDateTime to);

  /**
   * 기간 내 요청이 많은 API Key를 순서대로 집계합니다. API Key 없이 들어온 요청은 제외합니다.
   *
   * @param limit 가져올 최대 키 수
   */
  List<ApiKeyRequestVolume> aggregateTopApiKeys(LocalDateTime from, LocalDateTime to, int limit);
}
