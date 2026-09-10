package app.application.filter;

import app.config.LocalCacheType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 사전이 실제로 바뀌면 이전 사전으로 계산한 필터 결과 캐시를 비웁니다.
 *
 * <p>Caffeine 캐시는 인스턴스마다 독립적이므로, 각 인스턴스가 자신의 재동기화 결과에 반응해 스스로 비웁니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FilterResultCacheEvictor {

  private final CacheManager cacheManager;

  @EventListener
  public void onDictionaryReloaded(ProfanityDictionaryReloadedEvent event) {
    Cache cache = cacheManager.getCache(LocalCacheType.REQUEST_FILTER.getCacheName());
    if (cache == null) {
      log.warn("[FilterCache] {} 캐시가 없어 무효화를 건너뜁니다.", LocalCacheType.REQUEST_FILTER.getCacheName());
      return;
    }
    cache.clear();
    log.info("[FilterCache] 사전 변경으로 필터 결과 캐시를 비웠습니다. reloadedAt={}", event.reloadedAt());
  }
}
