package app.storage.rds;

import app.core.data.Pair;
import app.core.data.constant.Mode;
import app.domain.record.ApiKeyRequestVolume;
import app.domain.record.DailyRequestVolume;
import app.domain.record.ModeRequestVolume;
import app.domain.record.RecordRepository;
import app.domain.record.Records;
import app.domain.record.RequestVolume;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaRecordRepository extends RecordRepository, JpaRepository<Records, Long> {

  // 검출 여부는 아래 집계 네 곳 모두 words가 null이 아니고 빈 문자열도 아닌 조건으로 판정한다.
  // 검출이 없으면 빈 문자열이 저장되고, V4 이전에 쌓인 행에는 null이 들어 있을 수 있다.

  @Override
  @Query(
      """
            SELECT new app.core.data.Pair(COUNT(r.id),COUNT(case when length( r.words) > 0 then 1 else null end))
            FROM records r
            WHERE r.apiKeyHash = :apiKeyHash
            AND r.createdAt >= :yesterday
            """)
  Pair<Long, Long> getApiKeyDailyUsageStatistics(String apiKeyHash, LocalDateTime yesterday);

  @Override
  @Query("SELECT r FROM records r WHERE r.trackingId = :trackingId")
  Optional<Records> findByTrackingId(UUID trackingId);

  @Override
  default RequestVolume countRequestVolume(LocalDateTime from, LocalDateTime to) {
    List<Object[]> rows = sumRequestVolume(from, to);
    if (rows.isEmpty()) {
      return RequestVolume.empty();
    }
    Object[] row = rows.get(0);
    return new RequestVolume(AggregateValues.toLong(row[0]), AggregateValues.toLong(row[1]));
  }

  @Query(
      """
      select count(r.id),
             sum(case when r.words is not null and r.words <> '' then 1L else 0L end)
      from records r
      where r.createdAt >= :from and r.createdAt < :to
      """)
  List<Object[]> sumRequestVolume(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  @Override
  default List<DailyRequestVolume> aggregateDailyVolume(
      LocalDateTime from, LocalDateTime to, LocalDate fromDate) {
    return sumDailyVolume(from, to).stream()
        .map(
            row ->
                new DailyRequestVolume(
                    fromDate.plusDays(AggregateValues.toLong(row[0])),
                    AggregateValues.toLong(row[1]),
                    AggregateValues.toLong(row[2])))
        .toList();
  }

  /**
   * 하루 단위 집계만 네이티브 질의를 사용합니다. created_at을 날짜로 자르는 대신 시작 경계로부터 몇 번째 날인지를 셉니다.
   *
   * <p>created_at이 어떤 시간대의 벽시계로 저장되는지는 JVM 시간대와 JDBC 드라이버 설정에 함께 좌우되므로 애플리케이션이 단정할 수 없습니다. 경계와 같은
   * 기준으로 날짜 차이를 재면 저장 시간대를 몰라도 일자가 정확히 나뉩니다. 초 단위 차이를 하루로 나누는 방식은 자정 직전 1초 안에 들어온 요청을 다음 날로 밀어내므로
   * 쓰지 않습니다.
   */
  @Query(
      value =
          """
          select datediff(r.created_at, :from) as day_offset,
                 count(*) as total_requests,
                 sum(case when r.words is not null and r.words <> '' then 1 else 0 end) as detected_requests
          from records r
          where r.created_at >= :from and r.created_at < :to
          group by day_offset
          order by day_offset
          """,
      nativeQuery = true)
  List<Object[]> sumDailyVolume(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  @Override
  default List<ModeRequestVolume> aggregateModeVolume(LocalDateTime from, LocalDateTime to) {
    return sumModeVolume(from, to).stream()
        .map(row -> new ModeRequestVolume((Mode) row[0], AggregateValues.toLong(row[1])))
        .toList();
  }

  @Query(
      """
      select r.mode, count(r.id)
      from records r
      where r.createdAt >= :from and r.createdAt < :to
      group by r.mode
      """)
  List<Object[]> sumModeVolume(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  @Override
  default List<ApiKeyRequestVolume> aggregateTopApiKeys(
      LocalDateTime from, LocalDateTime to, int limit) {
    return sumApiKeyVolume(from, to, Limit.of(limit)).stream()
        .map(
            row ->
                new ApiKeyRequestVolume(
                    (String) row[0],
                    AggregateValues.toLong(row[1]),
                    AggregateValues.toLong(row[2])))
        .toList();
  }

  @Query(
      """
      select r.apiKeyHash,
             count(r.id),
             sum(case when r.words is not null and r.words <> '' then 1L else 0L end)
      from records r
      where r.apiKeyHash is not null
        and r.createdAt >= :from and r.createdAt < :to
      group by r.apiKeyHash
      order by count(r.id) desc
      """)
  List<Object[]> sumApiKeyVolume(
      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Limit limit);

  /** 집계 질의가 돌려주는 숫자와 날짜 타입을 도메인 타입으로 맞춥니다. */
  final class AggregateValues {

    private AggregateValues() {}

    static long toLong(Object value) {
      return value == null ? 0L : ((Number) value).longValue();
    }
  }
}
