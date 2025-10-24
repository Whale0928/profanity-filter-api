package app.application.client;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.client.TemporaryApiKey;
import app.domain.client.TemporaryApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

/**
 * 임시 API 키 생성 및 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemporaryApiKeyService {
    
    private static final int MAX_ISSUANCE_PER_IP = 3; // IP당 하루 최대 3회 발급
    private static final int DEFAULT_USAGE_LIMIT = 10; // 기본 10회 사용 가능
    private static final int KEY_EXPIRE_HOURS = 24; // 24시간 유효
    private static final SecureRandom secureRandom = new SecureRandom();
    
    private final TemporaryApiKeyRepository temporaryApiKeyRepository;

    /**
     * 임시 API 키 발급
     * @param ipAddress 요청자 IP 주소
     * @return 발급된 임시 API 키
     */
    public TemporaryApiKey issueTemporaryKey(String ipAddress) {
        // IP별 일일 발급 제한 확인
        validateIpIssuanceLimit(ipAddress);
        
        // 임시 키 생성
        String apiKey = generateTemporaryKey();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        
        TemporaryApiKey temporaryApiKey = TemporaryApiKey.builder()
                .apiKey(apiKey)
                .ipAddress(ipAddress)
                .remainingCount(DEFAULT_USAGE_LIMIT)
                .issuedAt(now)
                .expiredAt(now.plusHours(KEY_EXPIRE_HOURS))
                .build();
        
        // Redis에 저장
        temporaryApiKeyRepository.save(temporaryApiKey);
        
        // IP별 발급 카운트 증가
        temporaryApiKeyRepository.incrementIpIssuanceCount(ipAddress);
        
        log.info("임시 API 키 발급 완료: IP={}, Key={}, UsageLimit={}", 
                ipAddress, apiKey.substring(0, 10) + "...", DEFAULT_USAGE_LIMIT);
        
        return temporaryApiKey;
    }
    
    /**
     * 임시 API 키 유효성 검증 및 사용 처리
     * @param apiKey API 키
     * @return 유효하면 true
     */
    public boolean validateAndUse(String apiKey) {
        return temporaryApiKeyRepository.findByApiKey(apiKey)
                .map(tempKey -> {
                    if (!tempKey.isValid()) {
                        log.debug("임시 API 키 만료 또는 사용 횟수 초과: {}", apiKey);
                        temporaryApiKeyRepository.delete(apiKey);
                        return false;
                    }
                    
                    // 사용 횟수 감소
                    int remaining = tempKey.decrementUsage();
                    temporaryApiKeyRepository.save(tempKey);
                    
                    log.debug("임시 API 키 사용: {}, 남은 횟수: {}", apiKey, remaining);
                    
                    if (remaining == 0) {
                        // 사용 횟수 소진 시 삭제
                        temporaryApiKeyRepository.delete(apiKey);
                    }
                    
                    return true;
                })
                .orElse(false);
    }
    
    /**
     * IP별 발급 제한 검증
     * // TODO: IP 기반 제한은 프록시/VPN 등으로 우회 가능. 추가 보안 고려 필요
     */
    private void validateIpIssuanceLimit(String ipAddress) {
        int todayIssuances = temporaryApiKeyRepository.countTodayIssuancesByIp(ipAddress);
        if (todayIssuances >= MAX_ISSUANCE_PER_IP) {
            log.warn("IP별 일일 발급 제한 초과: IP={}, Count={}", ipAddress, todayIssuances);
            throw new BusinessException(
                    StatusCode.TOO_MANY_REQUESTS, 
                    "일일 임시 키 발급 제한을 초과했습니다. 내일 다시 시도해주세요."
            );
        }
    }
    
    /**
     * 임시 API 키 생성 (짧고 간단한 형태)
     */
    private String generateTemporaryKey() {
        byte[] randomBytes = new byte[12];
        secureRandom.nextBytes(randomBytes);
        return "temp_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
