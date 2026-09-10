package app.application.apikey;

import app.domain.apikey.ApiKey;
import app.domain.apikey.ApiKeyRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * API Key의 마지막 사용 시각을 기록합니다.
 *
 * <p>인증은 모든 외부 API 요청 경로에 있으므로 매 요청 UPDATE는 처리량에 직접 영향을 줍니다. 마지막 기록에서 {@link #RECORD_INTERVAL} 이상
 * 지난 경우에만 갱신하며, 관리자 화면의 마지막 사용 시각은 그만큼 지연될 수 있습니다.
 */
@Service
@RequiredArgsConstructor
public class ApiKeyUsageRecorder {

  public static final Duration RECORD_INTERVAL = Duration.ofSeconds(60);
  private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

  private final ApiKeyRepository apiKeyRepository;
  private final Clock loginAuthClock;

  /** 서비스 표준 시간대 기준 현재 시각입니다. 호출자가 기록 필요 여부를 먼저 판단할 때 사용합니다. */
  public LocalDateTime now() {
    return LocalDateTime.ofInstant(loginAuthClock.instant(), SERVICE_ZONE)
        .truncatedTo(ChronoUnit.MICROS);
  }

  /**
   * 마지막 사용 시각을 조건부로 갱신합니다.
   *
   * <p>호출자는 조회 커넥션을 반환한 후 이 짧은 트랜잭션을 실행합니다. {@link ApiKey#isUsageRecordStale}로 필요할 때만 호출하고, 잠금 후 다시
   * 시각을 확인하여 동시 요청의 중복 갱신을 막습니다.
   *
   * @return 실제로 갱신했으면 true
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean recordUsage(UUID apiKeyId) {
    LocalDateTime now = now();
    return apiKeyRepository
        .findByIdForUpdate(apiKeyId)
        .filter(apiKey -> apiKey.markUsedAt(now, RECORD_INTERVAL))
        .map(apiKeyRepository::save)
        .isPresent();
  }
}
