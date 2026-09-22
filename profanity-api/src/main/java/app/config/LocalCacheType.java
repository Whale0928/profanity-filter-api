package app.config;

import app.application.whitelist.WhitelistReader;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LocalCacheType {
  REQUEST_FILTER("request_filter", 60 * 60 * 24, 1000),
  REQUEST_CLIENT_INFO("request_client_info", 60 * 60 * 24, 100),
  // 허용 단어 그룹 본문. 다른 인스턴스의 수정은 만료로만 반영되므로 사전 동기화 주기와 같은 1분으로 둔다.
  WHITELIST_WORDS(WhitelistReader.CACHE_NAME, 60, 1000);

  private final String cacheName;
  private final int secsToExpireAfterWrite;
  private final int entryMaxSize;
}
