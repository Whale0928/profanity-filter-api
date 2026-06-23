package app.security.filter;

import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpMethod;

@Getter
public enum ExcludePath {
  CLIENTS("clients/register", "클라이언트 관련 API", List.of(HttpMethod.POST)),
  RESOURCE("resource", "리소스 관련 API", List.of(HttpMethod.GET)),
  EMAIL("send-email", "이메일 관련 API", List.of(HttpMethod.GET, HttpMethod.PUT)),
  HEALTH("health", "헬스 체크 API", List.of(HttpMethod.GET)),
  PING("ping", "헬스 체크 API", List.of(HttpMethod.GET)),
  OPENAPI("openapi.json", "OpenAPI JSON 스펙", List.of(HttpMethod.GET)),
  OPENAPI_MARKDOWN("openapi/", "OpenAPI Markdown 문서", List.of(HttpMethod.GET)),
  LLMS(List.of("llms.txt", "llm.txt"), "LLM 문서 색인", List.of(HttpMethod.GET)),
  ;

  private final List<String> paths;
  private final String description;
  private final List<HttpMethod> method;

  ExcludePath(String path, String description, List<HttpMethod> method) {
    this(List.of(path), description, method);
  }

  ExcludePath(List<String> paths, String description, List<HttpMethod> method) {
    this.paths = paths;
    this.description = description;
    this.method = method;
  }

  public static List<ExcludePath> getPaths() {
    return List.of(ExcludePath.values());
  }

  public static boolean isExcluded(String path) {
    return getPaths().stream().anyMatch(excludePath -> excludePath.isPathMatch(path));
  }

  /** 가능한 메소드 타입인지 확인 같은 경우 true , 다른 경우 false */
  public boolean isPossibleMethod(String method) {
    return this.getMethod().stream().anyMatch(httpMethod -> httpMethod.matches(method));
  }

  public boolean isMatch(String path, String method) {
    boolean contains = isPathMatch(path);
    boolean b = isPossibleMethod(method);
    return contains && b;
  }

  private boolean isPathMatch(String path) {
    return this.paths.stream().anyMatch(excludePath -> path.contains("/" + excludePath));
  }
}
