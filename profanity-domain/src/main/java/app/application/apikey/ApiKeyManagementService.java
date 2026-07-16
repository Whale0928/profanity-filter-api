package app.application.apikey;

import app.application.client.KeyGenerator;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.apikey.ApiKey;
import app.domain.apikey.ApiKeyRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiKeyManagementService {
  private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

  private final ApiKeyRepository apiKeyRepository;
  private final KeyGenerator keyGenerator;
  private final Clock loginAuthClock;

  @Transactional(readOnly = true)
  public List<ApiKeyView> findAll(UUID userId) {
    return apiKeyRepository.findAllByUserIdOrderByIssuedAtDesc(userId).stream()
        .map(ApiKeyView::from)
        .toList();
  }

  @Transactional
  public IssuedApiKey issue(UUID userId, String email, CreateApiKeyCommand command) {
    GeneratedKey generated = generateUniqueKey();
    ApiKey apiKey =
        ApiKey.issue(
            userId,
            command.name(),
            email,
            generated.hash(),
            generated.hint(),
            command.issuerInfo(),
            command.note(),
            now());
    return new IssuedApiKey(ApiKeyView.from(apiKeyRepository.save(apiKey)), generated.plaintext());
  }

  @Transactional
  public IssuedApiKey reissue(UUID userId, UUID apiKeyId) {
    ApiKey current = requireOwned(apiKeyId, userId);
    if (!current.isActive()) {
      throw new BusinessException(StatusCode.API_KEY_ALREADY_EXPIRED);
    }
    GeneratedKey generated = generateUniqueKey();
    ApiKey replacement = current.reissue(generated.hash(), generated.hint(), now());
    apiKeyRepository.save(current);
    return new IssuedApiKey(
        ApiKeyView.from(apiKeyRepository.save(replacement)), generated.plaintext());
  }

  @Transactional
  public ApiKeyView expire(UUID userId, UUID apiKeyId) {
    ApiKey apiKey = requireOwned(apiKeyId, userId);
    apiKey.expire(now());
    return ApiKeyView.from(apiKeyRepository.save(apiKey));
  }

  private ApiKey requireOwned(UUID apiKeyId, UUID userId) {
    return apiKeyRepository
        .findByIdAndUserId(apiKeyId, userId)
        .orElseThrow(() -> new BusinessException(StatusCode.API_KEY_NOT_FOUND));
  }

  private GeneratedKey generateUniqueKey() {
    try {
      String plaintext = keyGenerator.generateApiKey();
      String hash = keyGenerator.hashApiKey(plaintext);
      if (apiKeyRepository.existsByKeyHash(hash)) {
        throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR);
      }
      return new GeneratedKey(plaintext, hash, keyGenerator.keyHint(plaintext));
    } catch (NoSuchAlgorithmException exception) {
      throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR);
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.ofInstant(loginAuthClock.instant(), SERVICE_ZONE);
  }

  private record GeneratedKey(String plaintext, String hash, String hint) {}

  public record CreateApiKeyCommand(String name, String issuerInfo, String note) {}

  public record IssuedApiKey(
      @Schema(description = "발급된 API Key 메타데이터") ApiKeyView key,
      @Schema(description = "이 응답에서만 한 번 제공되는 API Key 원문", example = "pf_sample_issued_api_key")
          String apiKey) {}

  public record ApiKeyView(
      @Schema(description = "API Key 식별자") UUID id,
      @Schema(description = "사용자가 지정한 API Key 이름", example = "운영 서버") String name,
      @Schema(description = "SSO 대표 이메일", example = "user@example.com") String email,
      @Schema(description = "목록 표시용 마스킹 값", example = "AbCdEf...1234") String keyHint,
      @Schema(description = "발급자 정보") String issuerInfo,
      @Schema(description = "선택 메모") String note,
      @Schema(description = "API Key 권한") List<String> permissions,
      @Schema(description = "ACTIVE 또는 EXPIRED") String status,
      @Schema(description = "발급 시각") LocalDateTime issuedAt,
      @Schema(description = "만료 시각") LocalDateTime expiredAt,
      @Schema(description = "기존 일일 집계 요청 수") long requestCount) {
    private static ApiKeyView from(ApiKey apiKey) {
      return new ApiKeyView(
          apiKey.getId(),
          apiKey.getName(),
          apiKey.getEmail(),
          apiKey.getKeyHint(),
          apiKey.getIssuerInfo(),
          apiKey.getNote(),
          apiKey.plainPermissions(),
          apiKey.isActive() ? "ACTIVE" : "EXPIRED",
          apiKey.getIssuedAt(),
          apiKey.getExpiredAt(),
          apiKey.getRequestCount());
    }
  }
}
