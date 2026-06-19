package app.web.response;

import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 프록시(Cloudflare) 경유 호스트로 들어온 요청에만 meta에 처리 경로를 표기한다. 대상 호스트는 app.response.proxied-host 로 설정하며, 비어
 * 있으면 비활성화된다.
 */
@Component
public class HostResponseCustomizer implements ResponseCustomizer {
  private final String proxiedHost;

  public HostResponseCustomizer(@Value("${app.response.proxied-host:}") String proxiedHost) {
    // 비교 일관성을 위해 소문자로 정규화 (RequestContext.host 도 소문자 정규화됨)
    this.proxiedHost = proxiedHost == null ? "" : proxiedHost.trim().toLowerCase(Locale.ROOT);
  }

  @Override
  public boolean supports(RequestContext context) {
    return !proxiedHost.isEmpty() && context != null && proxiedHost.equals(context.host());
  }

  @Override
  public void customize(Map<String, Object> meta, RequestContext context) {
    meta.put("servedVia", context.host());
  }
}
