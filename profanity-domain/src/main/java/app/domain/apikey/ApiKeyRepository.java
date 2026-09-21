package app.domain.apikey;

import app.domain.support.PageResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository {
  ApiKey save(ApiKey apiKey);

  List<ApiKey> findAll();

  List<ApiKey> findAllByUserIdOrderByIssuedAtDesc(UUID userId);

  Optional<ApiKey> findById(UUID id);

  /** 재발급과 관리자 폐기가 겹칠 때 마지막 쓰기가 유효 키를 되살리지 않도록 행을 잠그고 조회합니다. */
  Optional<ApiKey> findByIdForUpdate(UUID id);

  Optional<ApiKey> findByIdAndUserId(UUID id, UUID userId);

  /**
   * 소유자의 재발급 및 만료가 관리자 폐기와 겹칠 때 행을 잠그고 조회합니다.
   *
   * <p>잠그지 않으면 폐기 직후의 재발급이 새 유효 키를 만들어 폐기가 무력화됩니다.
   */
  Optional<ApiKey> findByIdAndUserIdForUpdate(UUID id, UUID userId);

  /**
   * 관리자 API Key 목록을 조회합니다.
   *
   * @param query 이름, 발급 이메일, 표시용 힌트에 대한 부분 일치 검색어. null이면 전체입니다.
   * @param activeOnly true면 유효한 키만, false면 만료된 키만, null이면 전체입니다.
   */
  PageResult<ApiKey> searchForAdmin(String query, Boolean activeOnly, int page, int size);

  /** 발급된 전체 API Key 수입니다. 만료된 키를 포함합니다. */
  long countApiKeys();

  /** 아직 만료되지 않은 API Key 수입니다. */
  long countActiveApiKeys();

  /** 해시 목록으로 API Key를 조회합니다. 통계에서 집계한 해시에 이름과 소유자를 붙일 때 사용합니다. */
  List<ApiKey> findAllByKeyHashIn(Collection<String> keyHashes);

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
