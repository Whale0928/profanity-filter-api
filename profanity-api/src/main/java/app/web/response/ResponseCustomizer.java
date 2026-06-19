package app.web.response;

import java.util.Map;

/** 조건(supports)에 따라 응답 meta를 가공(customize)하는 단위. 새 커스터마이저를 구현체로 추가하면 등록되어 적용된다. */
public interface ResponseCustomizer {
  boolean supports(RequestContext context);

  void customize(Map<String, Object> meta, RequestContext context);
}
