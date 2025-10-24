package app.domain.client;

import java.util.Optional;

/**
 * 임시 API 키 저장소 인터페이스
 */
public interface TemporaryApiKeyRepository {
    
    /**
     * 임시 API 키 저장
     * @param temporaryApiKey 임시 API 키 객체
     */
    void save(TemporaryApiKey temporaryApiKey);
    
    /**
     * API 키로 조회
     * @param apiKey API 키
     * @return 임시 API 키 객체
     */
    Optional<TemporaryApiKey> findByApiKey(String apiKey);
    
    /**
     * IP 주소별 오늘 발급 횟수 조회
     * @param ipAddress IP 주소
     * @return 발급 횟수
     */
    int countTodayIssuancesByIp(String ipAddress);
    
    /**
     * IP 주소별 발급 횟수 증가
     * @param ipAddress IP 주소
     */
    void incrementIpIssuanceCount(String ipAddress);
    
    /**
     * 임시 API 키 삭제
     * @param apiKey API 키
     */
    void delete(String apiKey);
}
