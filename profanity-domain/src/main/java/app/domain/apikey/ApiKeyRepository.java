package app.domain.apikey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository {
  ApiKey save(ApiKey apiKey);

  List<ApiKey> findAll();

  List<ApiKey> findAllByUserIdOrderByIssuedAtDesc(UUID userId);

  Optional<ApiKey> findByIdAndUserId(UUID id, UUID userId);

  Optional<ApiKey> findByKeyHash(String keyHash);

  boolean existsByKeyHash(String keyHash);

  int claimUnownedByEmail(UUID userId, String email);

  /**
   * 기존 누적 요청 횟수 집계를 갱신합니다.
   *
   * @deprecated 사용량 수집 중단을 검토 중이며 신규 기능에서 사용하지 않습니다.
   */
  @Deprecated(forRemoval = true)
  void updateRequestCount();
}
