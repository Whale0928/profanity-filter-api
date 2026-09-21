package app.application.admin;

import app.application.admin.AdminStatisticsView.DailyView;
import app.application.admin.AdminStatisticsView.ModeView;
import app.application.admin.AdminStatisticsView.OperationsView;
import app.application.admin.AdminStatisticsView.PeriodView;
import app.application.admin.AdminStatisticsView.SummaryView;
import app.application.admin.AdminStatisticsView.TopApiKeyView;
import app.core.data.constant.Mode;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.apikey.ApiKey;
import app.domain.apikey.ApiKeyRepository;
import app.domain.audit.AdminAuditLogRepository;
import app.domain.inquiry.InquiryRepository;
import app.domain.inquiry.InquiryStatus;
import app.domain.manage.WordManagementRepository;
import app.domain.profanity.ProfanityRepository;
import app.domain.record.ApiKeyRequestVolume;
import app.domain.record.DailyRequestVolume;
import app.domain.record.ModeRequestVolume;
import app.domain.record.RecordRepository;
import app.domain.record.RequestVolume;
import app.domain.user.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 통계 개요를 집계합니다.
 *
 * <p>요청 지표의 원본은 records 한 곳입니다. 수집이 중단될 예정인 client_reports와 api_keys.request_count에는 의존하지 않습니다.
 *
 * <p>캐시에 적중한 요청은 필터 핸들러를 거치지 않아 records에 남지 않습니다. 따라서 여기서 세는 요청 수는 실제 호출 수가 아니라 기록된 요청 수입니다.
 *
 * <p>시간대가 테이블마다 다릅니다. users와 admin_audit_logs는 UTC Instant로 저장되고, records.created_at과 api_keys의 시각은
 * LocalDateTime이라 서비스 시간대(Asia/Seoul)의 벽시계가 들어갑니다. 운영은 JDBC URL의 serverTimezone과 JVM 시간대를 모두
 * Asia/Seoul에 맞춰 드라이버 변환량을 0으로 두고 있습니다.
 *
 * <p>기간 경계는 서비스 시간대의 자정으로 정하고, records를 조회할 때는 저장 시각과 같은 방식으로 만든 LocalDateTime을 넘깁니다. 두 값이 같은 변환을
 * 거치므로 JVM 시간대 설정이 달라져도 경계 비교와 일자 구분은 어긋나지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

  private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
  private static final int TOP_API_KEY_LIMIT = 5;

  private final RecordRepository recordRepository;
  private final ApiKeyRepository apiKeyRepository;
  private final ProfanityRepository profanityRepository;
  private final InquiryRepository inquiryRepository;
  private final WordManagementRepository wordManagementRepository;
  private final UserAccountRepository userAccountRepository;
  private final AdminAuditLogRepository adminAuditLogRepository;
  private final Clock loginAuthClock;

  /**
   * 통계 개요를 조회합니다. 화면이 모든 지표를 한 번에 그리므로 한 트랜잭션에서 모두 읽습니다.
   *
   * @param days 조회 기간. null이면 7일입니다.
   * @throws BusinessException 허용하지 않는 기간인 경우
   */
  @Transactional(readOnly = true)
  public AdminStatisticsView load(Integer days) {
    StatisticsPeriod period = StatisticsPeriod.parse(days);

    Instant now = loginAuthClock.instant();
    LocalDate today = LocalDate.ofInstant(now, SERVICE_ZONE);
    LocalDate fromDate = today.minusDays(period.days() - 1L);
    Instant fromInstant = fromDate.atStartOfDay(SERVICE_ZONE).toInstant();
    Instant toInstant = today.plusDays(1).atStartOfDay(SERVICE_ZONE).toInstant();

    ZoneId storageZone = recordStorageZone();
    LocalDateTime recordFrom = LocalDateTime.ofInstant(fromInstant, storageZone);
    LocalDateTime recordTo = LocalDateTime.ofInstant(toInstant, storageZone);

    RequestVolume volume = recordRepository.countRequestVolume(recordFrom, recordTo);

    return new AdminStatisticsView(
        new PeriodView(period.days(), fromDate, today),
        summary(volume),
        daily(recordFrom, recordTo, fromDate, today),
        modes(recordFrom, recordTo, volume.totalRequests()),
        operations(fromInstant),
        topApiKeys(recordFrom, recordTo));
  }

  /**
   * records.created_at이 저장되는 시간대입니다.
   *
   * <p>@CreationTimestamp가 LocalDateTime을 채우므로 JVM 기본 시간대의 벽시계가 그대로 들어갑니다. 운영은 UTC, 로컬 개발은 KST로
   * 기록되는 차이를 여기서 흡수합니다.
   */
  private ZoneId recordStorageZone() {
    return ZoneId.systemDefault();
  }

  private SummaryView summary(RequestVolume volume) {
    return new SummaryView(
        volume.totalRequests(),
        volume.detectedRequests(),
        percentage(volume.detectedRequests(), volume.totalRequests()),
        apiKeyRepository.countActiveApiKeys(),
        apiKeyRepository.countApiKeys(),
        profanityRepository.countUsedWords(),
        profanityRepository.countAll());
  }

  /** 요청이 없던 날에도 화면이 같은 개수의 막대를 그리도록 기간 전체를 0으로 채운 뒤 집계 값을 덮어씁니다. */
  private List<DailyView> daily(
      LocalDateTime recordFrom, LocalDateTime recordTo, LocalDate fromDate, LocalDate today) {
    Map<LocalDate, DailyRequestVolume> aggregated =
        recordRepository.aggregateDailyVolume(recordFrom, recordTo, fromDate).stream()
            .collect(
                Collectors.toMap(
                    DailyRequestVolume::date, Function.identity(), (first, second) -> first));

    return fromDate
        .datesUntil(today.plusDays(1))
        .map(
            date -> {
              DailyRequestVolume found = aggregated.get(date);
              return found == null
                  ? new DailyView(date, 0L, 0L)
                  : new DailyView(date, found.totalRequests(), found.detectedRequests());
            })
        .toList();
  }

  /** 요청이 없던 모드도 화면에서 비교할 수 있도록 0으로 포함하고 요청이 많은 순으로 정렬합니다. */
  private List<ModeView> modes(
      LocalDateTime recordFrom, LocalDateTime recordTo, long totalRequests) {
    Map<Mode, Long> aggregated =
        recordRepository.aggregateModeVolume(recordFrom, recordTo).stream()
            .collect(
                Collectors.toMap(
                    ModeRequestVolume::mode,
                    ModeRequestVolume::totalRequests,
                    (first, second) -> first + second));

    return Arrays.stream(Mode.values())
        .map(
            mode -> {
              long count = aggregated.getOrDefault(mode, 0L);
              return new ModeView(mode.name(), count, percentage(count, totalRequests));
            })
        .sorted(Comparator.comparingLong(ModeView::totalRequests).reversed())
        .toList();
  }

  private OperationsView operations(Instant fromInstant) {
    return new OperationsView(
        inquiryRepository.countByStatus(InquiryStatus.RECEIVED),
        wordManagementRepository.countPendingRequests(),
        userAccountRepository.countCreatedSince(fromInstant),
        adminAuditLogRepository.countCreatedSince(fromInstant));
  }

  /**
   * 요청이 많은 API Key를 집계하고 키 이름과 소유자를 채웁니다.
   *
   * <p>records는 해시만 보관하므로 집계한 해시로 api_keys를 다시 조회합니다. 폐기된 키도 원장에 남아 있어 대부분 조회되지만, 원장에서 사라진 해시는 이름
   * 없이 건수만 돌려줍니다.
   */
  private List<TopApiKeyView> topApiKeys(LocalDateTime recordFrom, LocalDateTime recordTo) {
    List<ApiKeyRequestVolume> volumes =
        recordRepository.aggregateTopApiKeys(recordFrom, recordTo, TOP_API_KEY_LIMIT);
    if (volumes.isEmpty()) {
      return List.of();
    }

    Map<String, ApiKey> keys =
        apiKeyRepository
            .findAllByKeyHashIn(volumes.stream().map(ApiKeyRequestVolume::apiKeyHash).toList())
            .stream()
            .collect(
                Collectors.toMap(
                    ApiKey::getKeyHash, Function.identity(), (first, second) -> first));
    Map<UUID, String> ownerNames = ownerNames(keys.values());

    return volumes.stream()
        .map(
            volume -> {
              ApiKey key = keys.get(volume.apiKeyHash());
              return new TopApiKeyView(
                  key == null ? null : key.getId(),
                  key == null ? null : key.getName(),
                  key == null ? null : key.getKeyHint(),
                  key == null ? null : ownerNames.get(key.getUserId()),
                  volume.totalRequests(),
                  volume.detectedRequests(),
                  percentage(volume.detectedRequests(), volume.totalRequests()),
                  key == null ? null : key.getLastUsedAt());
            })
        .toList();
  }

  private Map<UUID, String> ownerNames(Collection<ApiKey> keys) {
    Set<UUID> ownerIds =
        keys.stream()
            .map(ApiKey::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    // 소유자가 연결되지 않은 과거 키의 userId가 null이므로 null 조회를 허용하는 HashMap을 돌려준다.
    Map<UUID, String> names = new HashMap<>();
    if (ownerIds.isEmpty()) {
      return names;
    }
    userAccountRepository
        .findAllByIdIn(ownerIds)
        .forEach(owner -> names.putIfAbsent(owner.getId(), owner.getDisplayName()));
    return names;
  }

  /** 백분율을 소수점 첫째 자리까지 반올림합니다. 모수가 0이면 0을 돌려줍니다. */
  private double percentage(long part, long total) {
    if (total <= 0L) {
      return 0.0d;
    }
    return Math.round(part * 1000.0d / total) / 10.0d;
  }

  /**
   * 조회 기간입니다. 화면이 제공하는 세 가지만 허용합니다.
   *
   * <p>임의의 일수를 받으면 상한 없는 범위 조회가 가능해지고, 값을 조용히 보정하면 관리자가 잘못된 기간을 보고도 알아채지 못합니다.
   */
  public enum StatisticsPeriod {
    DAYS_7(7),
    DAYS_30(30),
    DAYS_90(90);

    private final int days;

    StatisticsPeriod(int days) {
      this.days = days;
    }

    public int days() {
      return days;
    }

    /**
     * 요청 파라미터를 기간으로 바꿉니다. 값이 없으면 기본값인 7일입니다.
     *
     * @throws BusinessException 허용하지 않는 일수인 경우
     */
    public static StatisticsPeriod parse(Integer days) {
      if (days == null) {
        return DAYS_7;
      }
      for (StatisticsPeriod period : values()) {
        if (period.days == days) {
          return period;
        }
      }
      throw new BusinessException(StatusCode.BAD_REQUEST, "조회 기간은 7일, 30일, 90일 중 하나여야 합니다.");
    }
  }
}
