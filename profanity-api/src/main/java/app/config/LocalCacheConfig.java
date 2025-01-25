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
                .map(localCacheType ->
                        new CaffeineCache(
                                localCacheType.getCacheName(),
                                Caffeine.newBuilder()
                                        .recordStats()
                                        .expireAfterWrite(localCacheType.getSecsToExpireAfterWrite(), TimeUnit.SECONDS)
                                        .maximumSize(localCacheType.getEntryMaxSize())
                                        .build()))
                .toList();
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        log.info("{} :: cacheManager : {}", now(), cacheManager);
        return cacheManager;
    }
}
