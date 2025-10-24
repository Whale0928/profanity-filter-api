package app.redis;

import app.domain.client.TemporaryApiKey;
import app.domain.client.TemporaryApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 임시 API 키 Redis 저장소 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TemporaryApiKeyRedisRepository implements TemporaryApiKeyRepository {
    
    private static final String TEMP_KEY_PREFIX = "temp:apikey:";
    private static final String IP_COUNT_PREFIX = "temp:ip:count:";
    private static final long KEY_EXPIRE_HOURS = 24; // 24시간 후 자동 삭제
    
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(TemporaryApiKey temporaryApiKey) {
        String key = TEMP_KEY_PREFIX + temporaryApiKey.getApiKey();
        redisTemplate.opsForValue().set(key, temporaryApiKey, KEY_EXPIRE_HOURS, TimeUnit.HOURS);
        log.debug("임시 API 키 저장: {}, IP: {}", temporaryApiKey.getApiKey(), temporaryApiKey.getIpAddress());
    }

    @Override
    public Optional<TemporaryApiKey> findByApiKey(String apiKey) {
        String key = TEMP_KEY_PREFIX + apiKey;
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value instanceof TemporaryApiKey temporaryApiKey) {
            return Optional.of(temporaryApiKey);
        }
        return Optional.empty();
    }

    @Override
    public int countTodayIssuancesByIp(String ipAddress) {
        String key = getIpCountKey(ipAddress);
        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? (Integer) count : 0;
    }

    @Override
    public void incrementIpIssuanceCount(String ipAddress) {
        String key = getIpCountKey(ipAddress);
        Long newCount = redisTemplate.opsForValue().increment(key);
        
        if (newCount != null && newCount == 1) {
            // 첫 발급이면 자정까지의 TTL 설정
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
            long secondsUntilMidnight = Duration.between(now, midnight).getSeconds();
            redisTemplate.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);
        }
        log.debug("IP {} 오늘 발급 횟수: {}", ipAddress, newCount);
    }

    @Override
    public void delete(String apiKey) {
        String key = TEMP_KEY_PREFIX + apiKey;
        redisTemplate.delete(key);
        log.debug("임시 API 키 삭제: {}", apiKey);
    }
    
    private String getIpCountKey(String ipAddress) {
        String today = LocalDate.now().toString();
        return IP_COUNT_PREFIX + today + ":" + ipAddress;
    }
}
