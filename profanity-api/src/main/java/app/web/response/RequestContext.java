package app.web.response;

import java.util.Locale;
import org.springframework.http.server.ServerHttpRequest;

/** 응답 커스터마이징 판단에 사용하는 요청 컨텍스트. 조건이 늘면 필드를 확장한다. */
public record RequestContext(String host) {
  public static RequestContext from(ServerHttpRequest request) {
    String host = request.getURI().getHost();
    // 호스트명은 RFC상 case-insensitive 이므로 소문자로 정규화해 비교 일관성을 보장한다.
    return new RequestContext(host == null ? null : host.toLowerCase(Locale.ROOT));
  }
}
