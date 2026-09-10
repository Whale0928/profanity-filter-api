package app.application.apikey;

import app.application.client.KeyGenerator;
import app.core.data.response.constant.StatusCode;
import app.domain.apikey.ApiKey;
import app.domain.apikey.ApiKeyMetadata;
import app.domain.apikey.ApiKeyRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiKeyReader implements ApiKeyMetadataReader {
  private final ApiKeyRepository apiKeyRepository;
  private final KeyGenerator keyGenerator;
  private final ApiKeyUsageRecorder apiKeyUsageRecorder;

  @Override
  public ApiKeyMetadata read(String plaintextApiKey) {
    if (!keyGenerator.validateApiKey(plaintextApiKey)) {
      throw new IllegalArgumentException(StatusCode.INVALID_API_KEY.stringCode());
    }
    ApiKey apiKey =
        apiKeyRepository
            .findByKeyHash(keyGenerator.hashApiKey(plaintextApiKey))
            .filter(ApiKey::isActive)
            .orElseThrow(
                () -> new NoSuchElementException(StatusCode.NOT_FOUND_CLIENT.stringCode()));
    // 조회 커넥션을 반환한 뒤 기록한다. 바깥 트랜잭션을 유지하면 요청마다 연결 두 개가 필요하다.
    if (apiKey.isUsageRecordStale(apiKeyUsageRecorder.now(), ApiKeyUsageRecorder.RECORD_INTERVAL)) {
      try {
        apiKeyUsageRecorder.recordUsage(apiKey.getId());
      } catch (DataAccessException | TransactionException exception) {
        // 사용 통계 기록 실패가 이미 검증된 자격증명을 무효로 만들지는 않는다.
        log.warn("API Key 최근 사용 기록 실패: {}", exception.getClass().getSimpleName());
      }
    }
    return new ApiKeyMetadata(
        apiKey.getId(),
        apiKey.getEmail(),
        apiKey.getIssuerInfo(),
        apiKey.plainPermissions(),
        apiKey.getIssuedAt().toString(),
        apiKey.getKeyHash());
  }
}
