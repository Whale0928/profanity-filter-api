package app.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.application.admin.AdminStatisticsView.DailyView;
import app.application.admin.AdminStatisticsView.ModeView;
import app.application.admin.AdminStatisticsView.TopApiKeyView;
import app.core.data.constant.Mode;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.InMemoryAdminAuditLogRepository;
import app.domain.InMemoryApiKeyRepository;
import app.domain.InMemoryInquiryRepository;
import app.domain.InMemoryUserAccountRepository;
import app.domain.InMemoryWordManagementRepository;
import app.domain.InmemoryProfanityRepository;
import app.domain.InmemoryRecordRepository;
import app.domain.apikey.ApiKey;
import app.domain.audit.AdminAuditLog;
import app.domain.inquiry.Inquiry;
import app.domain.inquiry.InquiryStatus;
import app.domain.inquiry.InquiryType;
import app.domain.manage.WordManagementRequest;
import app.domain.profanity.ProfanityWord;
import app.domain.profanity.constant.isUsedType;
import app.domain.record.ApiKeyRequestVolume;
import app.domain.record.DailyRequestVolume;
import app.domain.record.ModeRequestVolume;
import app.domain.record.RequestVolume;
import app.domain.user.UserAccount;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminStatisticsServiceTest {

  /** 서비스 시간대(Asia/Seoul)로는 2026.09.21 정오입니다. */
  private static final Instant NOW = Instant.parse("2026-09-21T03:00:00Z");

  private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
  private static final LocalDate TODAY = LocalDate.of(2026, 9, 21);

  private final InmemoryRecordRepository recordRepository = new InmemoryRecordRepository();
  private final InMemoryApiKeyRepository apiKeyRepository = new InMemoryApiKeyRepository();
  private final InmemoryProfanityRepository profanityRepository = new InmemoryProfanityRepository();
  private final InMemoryInquiryRepository inquiryRepository = new InMemoryInquiryRepository();
  private final InMemoryWordManagementRepository wordManagementRepository =
      new InMemoryWordManagementRepository();
  private final InMemoryUserAccountRepository userAccountRepository =
      new InMemoryUserAccountRepository();
  private final InMemoryAdminAuditLogRepository auditLogRepository =
      new InMemoryAdminAuditLogRepository();

  private final AdminStatisticsService adminStatisticsService =
      new AdminStatisticsService(
          recordRepository,
          apiKeyRepository,
          profanityRepository,
          inquiryRepository,
          wordManagementRepository,
          userAccountRepository,
          auditLogRepository,
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Nested
  @DisplayName("조회 기간은")
  class Period {

    @Test
    @DisplayName("지정하지 않으면 오늘까지 7일로 집계한다")
    void load_withoutDays_usesSevenDays() {
      AdminStatisticsView view = adminStatisticsService.load(null);

      assertThat(view.period().days()).isEqualTo(7);
      assertThat(view.period().from()).isEqualTo(LocalDate.of(2026, 9, 15));
      assertThat(view.period().to()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("30일과 90일을 허용한다")
    void load_withAllowedDays_usesThatRange() {
      assertThat(adminStatisticsService.load(30).period().from())
          .isEqualTo(LocalDate.of(2026, 8, 23));
      assertThat(adminStatisticsService.load(90).period().from())
          .isEqualTo(LocalDate.of(2026, 6, 24));
    }

    @Test
    @DisplayName("허용하지 않는 일수는 조용히 보정하지 않고 거절한다")
    void load_withUnsupportedDays_throws() {
      assertThatThrownBy(() -> adminStatisticsService.load(14))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              thrown ->
                  assertThat(((BusinessException) thrown).getStatus().code())
                      .isEqualTo(StatusCode.BAD_REQUEST.code()));
    }

    @Test
    @DisplayName("서비스 시간대의 자정을 경계로 삼아 기록을 조회한다")
    void load_queriesRecordsOnServiceZoneMidnight() {
      adminStatisticsService.load(7);

      Instant expectedFrom = LocalDate.of(2026, 9, 15).atStartOfDay(SERVICE_ZONE).toInstant();
      Instant expectedTo = TODAY.plusDays(1).atStartOfDay(SERVICE_ZONE).toInstant();
      ZoneId storageZone = ZoneId.systemDefault();

      assertThat(recordRepository.requestedFrom())
          .isEqualTo(LocalDateTime.ofInstant(expectedFrom, storageZone));
      assertThat(recordRepository.requestedTo())
          .isEqualTo(LocalDateTime.ofInstant(expectedTo, storageZone));
    }

    @Test
    @DisplayName("일별 집계에 기간 시작 일자를 함께 넘겨 저장 시간대를 해석하지 않게 한다")
    void load_passesPeriodStartForDailyBuckets() {
      adminStatisticsService.load(7);

      assertThat(recordRepository.requestedFromDate()).isEqualTo(LocalDate.of(2026, 9, 15));
    }
  }

  @Nested
  @DisplayName("요약 지표는")
  class Summary {

    @Test
    @DisplayName("검출률을 소수점 첫째 자리까지 계산한다")
    void load_computesDetectionRate() {
      recordRepository.setRequestVolume(new RequestVolume(26_481L, 3_912L));

      AdminStatisticsView view = adminStatisticsService.load(7);

      assertThat(view.summary().totalRequests()).isEqualTo(26_481L);
      assertThat(view.summary().detectedRequests()).isEqualTo(3_912L);
      assertThat(view.summary().detectionRate()).isEqualTo(14.8d);
    }

    @Test
    @DisplayName("기록이 없으면 검출률을 0으로 둔다")
    void load_withoutRecords_returnsZeroRate() {
      AdminStatisticsView view = adminStatisticsService.load(7);

      assertThat(view.summary().totalRequests()).isZero();
      assertThat(view.summary().detectionRate()).isZero();
    }

    @Test
    @DisplayName("API Key와 사전 단어는 기간과 무관한 누적 값으로 센다")
    void load_countsCumulativeValues() {
      apiKeyRepository.save(apiKey("커뮤니티 웹", "hash-active"));
      ApiKey expired = apiKey("이전 프로젝트", "hash-expired");
      expired.expire(LocalDateTime.now());
      apiKeyRepository.save(expired);
      profanityRepository.save(ProfanityWord.create("사용중표현"));
      ProfanityWord unused = ProfanityWord.create("사용안함표현");
      unused.changeUsage(isUsedType.N, null, NOW);
      profanityRepository.save(unused);

      AdminStatisticsView view = adminStatisticsService.load(7);

      assertThat(view.summary().activeApiKeys()).isEqualTo(1L);
      assertThat(view.summary().totalApiKeys()).isEqualTo(2L);
      assertThat(view.summary().usedWords()).isEqualTo(1L);
      assertThat(view.summary().totalWords()).isEqualTo(2L);
    }
  }

  @Nested
  @DisplayName("일별 추이는")
  class Daily {

    @Test
    @DisplayName("요청이 없던 날도 0으로 채워 기간 전체를 돌려준다")
    void load_fillsMissingDaysWithZero() {
      recordRepository.setDailyVolumes(
          List.of(
              new DailyRequestVolume(LocalDate.of(2026, 9, 19), 4_470L, 702L),
              new DailyRequestVolume(TODAY, 3_668L, 577L)));

      List<DailyView> daily = adminStatisticsService.load(7).daily();

      assertThat(daily).hasSize(7);
      assertThat(daily.get(0).date()).isEqualTo(LocalDate.of(2026, 9, 15));
      assertThat(daily.get(0).totalRequests()).isZero();
      assertThat(daily.get(4).totalRequests()).isEqualTo(4_470L);
      assertThat(daily.get(4).detectedRequests()).isEqualTo(702L);
      assertThat(daily.get(6).date()).isEqualTo(TODAY);
      assertThat(daily.get(6).totalRequests()).isEqualTo(3_668L);
    }
  }

  @Nested
  @DisplayName("모드별 분포는")
  class Modes {

    @Test
    @DisplayName("요청이 없던 모드까지 포함해 요청이 많은 순으로 돌려준다")
    void load_includesEveryModeOrderedByVolume() {
      recordRepository.setRequestVolume(new RequestVolume(1_000L, 100L));
      recordRepository.setModeVolumes(
          List.of(
              new ModeRequestVolume(Mode.FILTER, 250L), new ModeRequestVolume(Mode.NORMAL, 750L)));

      List<ModeView> modes = adminStatisticsService.load(7).modes();

      assertThat(modes).hasSize(3);
      assertThat(modes.get(0).mode()).isEqualTo(Mode.NORMAL.name());
      assertThat(modes.get(0).share()).isEqualTo(75.0d);
      assertThat(modes.get(1).mode()).isEqualTo(Mode.FILTER.name());
      assertThat(modes.get(1).share()).isEqualTo(25.0d);
      assertThat(modes.get(2).mode()).isEqualTo(Mode.QUICK.name());
      assertThat(modes.get(2).totalRequests()).isZero();
    }
  }

  @Nested
  @DisplayName("운영 현황은")
  class Operations {

    @Test
    @DisplayName("대기 건수와 기간 내 활동을 함께 센다")
    void load_countsPendingWorkAndPeriodActivity() {
      inquiryRepository.save(
          Inquiry.fromLoginUser(InquiryType.GENERAL, "제목", "내용", UUID.randomUUID(), NOW));
      Inquiry replied =
          Inquiry.fromLoginUser(InquiryType.GENERAL, "답변된 문의", "내용", UUID.randomUUID(), NOW);
      replied.changeStatus(InquiryStatus.RESOLVED, NOW);
      inquiryRepository.save(replied);
      wordManagementRepository.save(
          WordManagementRequest.builder()
              .word("대기표현")
              .reason("사유")
              .severity("LOW")
              .requestType("ADD")
              .build());
      userAccountRepository.save(UserAccount.create("신규 사용자", "new@example.test", null, NOW));
      userAccountRepository.save(
          UserAccount.create("오래된 사용자", "old@example.test", null, NOW.minus(Duration.ofDays(30))));
      auditLogRepository.save(
          AdminAuditLog.record(
              UUID.randomUUID(), AdminAuditAction.WORD_CREATED, "WORD", "1", null, NOW));

      AdminStatisticsView.OperationsView operations = adminStatisticsService.load(7).operations();

      assertThat(operations.unansweredInquiries()).isEqualTo(1L);
      assertThat(operations.pendingWordRequests()).isEqualTo(1L);
      assertThat(operations.newUsers()).isEqualTo(1L);
      assertThat(operations.adminActions()).isEqualTo(1L);
    }
  }

  @Nested
  @DisplayName("API Key 상위 목록은")
  class TopApiKeys {

    @Test
    @DisplayName("집계한 해시에 키 이름과 소유자를 붙인다")
    void load_resolvesKeyNameAndOwner() {
      UserAccount owner = UserAccount.create("개발자 A", "dev-a@example.test", null, NOW);
      userAccountRepository.save(owner);
      ApiKey key =
          ApiKey.issue(
              owner.getId(),
              "커뮤니티 웹",
              "dev-a@example.test",
              "hash-community",
              "•••• A001",
              "test",
              null,
              LocalDateTime.of(2026, 9, 1, 9, 0));
      key.markUsedAt(LocalDateTime.of(2026, 9, 21, 11, 58), Duration.ZERO);
      apiKeyRepository.save(key);
      recordRepository.setApiKeyVolumes(
          List.of(new ApiKeyRequestVolume("hash-community", 9_842L, 1_514L)));

      List<TopApiKeyView> topApiKeys = adminStatisticsService.load(7).topApiKeys();

      assertThat(recordRepository.requestedTopLimit()).isEqualTo(5);
      assertThat(topApiKeys)
          .singleElement()
          .satisfies(
              top -> {
                assertThat(top.apiKeyId()).isEqualTo(key.getId());
                assertThat(top.name()).isEqualTo("커뮤니티 웹");
                assertThat(top.keyHint()).isEqualTo("•••• A001");
                assertThat(top.ownerName()).isEqualTo("개발자 A");
                assertThat(top.totalRequests()).isEqualTo(9_842L);
                assertThat(top.detectionRate()).isEqualTo(15.4d);
                // 관리자 키 목록과 같은 저장값을 그대로 내보낸다.
                assertThat(top.lastUsedAt()).isEqualTo(LocalDateTime.of(2026, 9, 21, 11, 58));
              });
    }

    @Test
    @DisplayName("키 원장에서 사라진 해시는 이름 없이 건수만 돌려준다")
    void load_withUnknownHash_keepsCountsOnly() {
      recordRepository.setApiKeyVolumes(List.of(new ApiKeyRequestVolume("hash-gone", 120L, 12L)));

      List<TopApiKeyView> topApiKeys = adminStatisticsService.load(7).topApiKeys();

      assertThat(topApiKeys)
          .singleElement()
          .satisfies(
              top -> {
                assertThat(top.apiKeyId()).isNull();
                assertThat(top.name()).isNull();
                assertThat(top.ownerName()).isNull();
                assertThat(top.totalRequests()).isEqualTo(120L);
                assertThat(top.detectionRate()).isEqualTo(10.0d);
              });
    }
  }

  private ApiKey apiKey(String name, String keyHash) {
    return ApiKey.issue(
        UUID.randomUUID(),
        name,
        name + "@example.test",
        keyHash,
        "•••• 0000",
        "test",
        null,
        LocalDateTime.of(2026, 9, 1, 9, 0));
  }
}
