package app.application.whitelist;

import app.domain.whitelist.WhitelistRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 필터 경로에서 허용 단어 그룹을 읽습니다.
 *
 * <p>그룹 단위로 짧게 캐시합니다. 요청이 그룹을 어떤 조합으로 지정하든 그룹마다 한 번만 읽으면 되므로 조합별로 따로 저장하지 않습니다. 인스턴스가 여럿이라 수정 즉시 모든
 * 인스턴스의 캐시를 비울 수는 없고, 다른 인스턴스는 캐시 만료로 새 내용을 받습니다.
 */
@Service
@RequiredArgsConstructor
public class WhitelistReader {

  public static final String CACHE_NAME = "whitelist_words";

  private final WhitelistRepository whitelistRepository;

  /**
   * 그룹을 읽습니다. 없는 그룹은 캐시하지 않습니다.
   *
   * @return 그룹이 없으면 null
   */
  @Cacheable(value = CACHE_NAME, key = "#id", unless = "#result == null")
  @Transactional(readOnly = true)
  public WhitelistSnapshot read(UUID id) {
    return whitelistRepository
        .findById(id)
        .map(
            whitelist ->
                new WhitelistSnapshot(
                    whitelist.getId(), whitelist.getUserId(), whitelist.comparisonKeys()))
        .orElse(null);
  }

  /** 그룹을 고치거나 지운 인스턴스에서는 기다리지 않고 바로 반영되도록 캐시를 비웁니다. */
  @CacheEvict(value = CACHE_NAME, key = "#id")
  public void evict(UUID id) {}
}
