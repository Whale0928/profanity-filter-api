package app.application.whitelist;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.apikey.ApiKey;
import app.domain.apikey.ApiKeyRepository;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 필터 요청이 지정한 허용 단어 그룹을 검증하고 허용 단어로 풀어냅니다.
 *
 * <p>잘못된 그룹 ID를 조용히 무시하지 않습니다. 무시하고 진행하면 고객은 허용 단어가 적용된 줄 알고 운영하게 됩니다.
 */
@Service
@RequiredArgsConstructor
public class WhitelistResolver {

  public static final int MAX_GROUPS_PER_REQUEST = 5;

  private final ApiKeyRepository apiKeyRepository;
  private final WhitelistReader whitelistReader;

  /**
   * 지정한 그룹들의 허용 단어를 합쳐서 돌려줍니다.
   *
   * @param apiKeyHash 요청에 쓰인 API Key의 해시
   * @param whitelistIds 적용할 그룹 ID. null이거나 비어 있으면 빈 집합을 돌려줍니다.
   * @throws BusinessException 그룹 수가 상한을 넘거나, API Key에 소유 계정이 없거나, 그룹이 없거나 다른 계정의 것인 경우
   */
  @Transactional(readOnly = true)
  public Set<String> resolve(String apiKeyHash, List<UUID> whitelistIds) {
    if (whitelistIds == null || whitelistIds.isEmpty()) {
      return Set.of();
    }
    Set<UUID> distinctIds = new LinkedHashSet<>(whitelistIds);
    if (distinctIds.contains(null)) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "허용 단어 그룹 ID가 비어 있습니다.");
    }
    if (distinctIds.size() > MAX_GROUPS_PER_REQUEST) {
      throw new BusinessException(
          StatusCode.WHITELIST_LIMIT_EXCEEDED,
          "요청 한 번에 지정할 수 있는 허용 단어 그룹은 최대 " + MAX_GROUPS_PER_REQUEST + "개입니다.");
    }

    UUID ownerId = ownerOf(apiKeyHash);

    Set<String> allowedWords = new HashSet<>();
    for (UUID id : distinctIds) {
      WhitelistSnapshot snapshot = whitelistReader.read(id);
      // 없는 그룹과 남의 그룹을 같은 코드로 응답해 다른 계정의 그룹이 존재하는지 알 수 없게 한다.
      if (snapshot == null || !ownerId.equals(snapshot.userId())) {
        throw new BusinessException(StatusCode.WHITELIST_NOT_FOUND);
      }
      allowedWords.addAll(snapshot.comparisonKeys());
    }
    return Set.copyOf(allowedWords);
  }

  private UUID ownerOf(String apiKeyHash) {
    if (apiKeyHash == null) {
      throw new BusinessException(StatusCode.WHITELIST_OWNER_REQUIRED);
    }
    // 소유 계정이 연결되지 않은 키는 userId가 null이라 map에서 빈 값이 되고 아래 예외로 이어진다.
    return apiKeyRepository
        .findByKeyHash(apiKeyHash)
        .map(ApiKey::getUserId)
        .orElseThrow(() -> new BusinessException(StatusCode.WHITELIST_OWNER_REQUIRED));
  }
}
