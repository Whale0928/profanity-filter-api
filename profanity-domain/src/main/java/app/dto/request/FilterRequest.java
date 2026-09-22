package app.dto.request;

import app.core.data.constant.Mode;
import java.util.Set;

/**
 * 필터 요청입니다.
 *
 * @param allowedWords 이 요청에서 검출하지 않을 단어. 비교 키로 정리된 값이며 허용 단어 그룹을 지정하지 않았으면 비어 있습니다.
 */
public record FilterRequest(
    String text,
    Mode mode,
    String apiKeyHash,
    String clientIp,
    String referrer,
    Set<String> allowedWords) {

  public FilterRequest {
    allowedWords = allowedWords == null ? Set.of() : Set.copyOf(allowedWords);
  }

  public static FilterRequest create(
      String text, Mode mode, String apiKeyHash, String clientIp, String referrer) {
    return new FilterRequest(text, mode, apiKeyHash, clientIp, referrer, Set.of());
  }

  public static FilterRequest create(
      String text,
      Mode mode,
      String apiKeyHash,
      String clientIp,
      String referrer,
      Set<String> allowedWords) {
    return new FilterRequest(text, mode, apiKeyHash, clientIp, referrer, allowedWords);
  }

  @Override
  public String toString() {
    return "FilterRequest{"
        + "text='"
        + text
        + '\''
        + ", mode="
        + mode
        + ", apiKeyHash='"
        + apiKeyHash
        + '\''
        + ", clientIp='"
        + clientIp
        + '\''
        + ", referrer='"
        + referrer
        + '\''
        + ", allowedWords="
        + allowedWords.size()
        + '}';
  }
}
