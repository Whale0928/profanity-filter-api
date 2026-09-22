package app.application.whitelist;

import java.util.Set;
import java.util.UUID;

/**
 * 필터 경로가 캐시에 두고 쓰는 허용 단어 그룹의 읽기 전용 사본입니다.
 *
 * @param id 그룹 ID
 * @param userId 그룹을 소유한 계정
 * @param comparisonKeys 비교 키로 정리한 허용 단어
 */
public record WhitelistSnapshot(UUID id, UUID userId, Set<String> comparisonKeys) {

  public WhitelistSnapshot {
    comparisonKeys = comparisonKeys == null ? Set.of() : Set.copyOf(comparisonKeys);
  }
}
