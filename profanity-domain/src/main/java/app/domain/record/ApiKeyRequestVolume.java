package app.domain.record;

/**
 * API Key별 요청 집계입니다.
 *
 * <p>records는 API Key의 원문이 아니라 SHA-256 해시만 보관하므로, 키 이름과 소유자는 이 해시로 api_keys를 다시 조회해 채웁니다.
 *
 * @param apiKeyHash API Key SHA-256 해시
 * @param totalRequests 해당 키로 기록된 요청 수
 * @param detectedRequests 해당 키에서 비속어가 검출된 요청 수
 */
public record ApiKeyRequestVolume(String apiKeyHash, long totalRequests, long detectedRequests) {}
