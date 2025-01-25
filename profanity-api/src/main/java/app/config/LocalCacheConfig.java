package app.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.time.LocalDateTime.now;

@Slf4j
@Configuration
@EnableCaching
public class LocalCacheConfig {
    @Bean
    public CacheManager cacheManager() {
        List<CaffeineCache> caches = Arrays.stream(LocalCacheType.values())
                .map(this::createFilterCache)
                .toList();
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        log.info("{} :: cacheManager : {}", now(), cacheManager);
        return cacheManager;
    }

    public CaffeineCache createFilterCache(LocalCacheType cacheType) {
        // 캐시 설정을 위한 Caffeine 빌더 생성
        Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder();

        // 캐시 통계 수집 활성화
        caffeineBuilder.recordStats();

        // 마지막 쓰기 이후 만료 시간 설정 (초 단위)
        caffeineBuilder.expireAfterWrite(
                cacheType.getSecsToExpireAfterWrite(),
                TimeUnit.SECONDS
        );

        // 최대 캐시 항목 수 설정
        caffeineBuilder.maximumWeight(cacheType.getEntryMaxSize())
                .weigher((k, v) -> 1); // LFU 방식 사용을 위한 가중치 설정

        var cache = caffeineBuilder.build();
        log.info("Cache stats for {}: {}", cacheType.getCacheName(), cache.stats());


        // CaffeineCache 객체 생성 및 반환
        return new CaffeineCache(
                cacheType.getCacheName(),
                cache
        );
    }

}
