package app.security.filter;

import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpMethod;

@Getter
public enum ExcludePath {
  RESOURCE("resource", "리소스 관련 API", List.of(HttpMethod.GET)),
  HEALTH("health", "헬스 체크 API", List.of(HttpMethod.GET)),
  PING("ping", "헬스 체크 API", List.of(HttpMethod.GET)),
  OPENAPI("openapi.json", "OpenAPI JSON 스펙", List.of(HttpMethod.GET)),
  OVERVIEW("overview.md", "API Overview Markdown 문서", List.of(HttpMethod.GET)),
  LLMS(List.of("llms.txt", "llm.txt"), "LLM 문서 색인", List.of(HttpMethod.GET)),
  SSO("sso", "SSO 정적 페이지", List.of(HttpMethod.GET)),
  OAUTH2("oauth2", "OAuth2 인증 시작", List.of(HttpMethod.GET)),
  OAUTH2_CALLBACK("login/oauth2/code", "OAuth2 callback", List.of(HttpMethod.GET)),
  AUTH_EXCHANGE("/api/v1/auth/exchange", "로그인 코드 교환", List.of(HttpMethod.POST), true),
  AUTH_CSRF("/api/v1/auth/csrf", "로그인 CSRF 토큰", List.of(HttpMethod.GET), true),
  AUTH_REFRESH("/api/v1/auth/refresh", "로그인 토큰 갱신", List.of(HttpMethod.POST), true),
  ;

  private final List<String> paths;
  private final String description;
  private final List<HttpMethod> method;
  private final boolean exactPath;

  ExcludePath(String path, String description, List<HttpMethod> method) {
    this(List.of(path), description, method, false);
  }

  ExcludePath(List<String> paths, String description, List<HttpMethod> method) {
    this(paths, description, method, false);
  }

  ExcludePath(String path, String description, List<HttpMethod> method, boolean exactPath) {
    this(List.of(path), description, method, exactPath);
  }

  ExcludePath(List<String> paths, String description, List<HttpMethod> method, boolean exactPath) {
    this.paths = paths;
    this.description = description;
    this.method = method;
    this.exactPath = exactPath;
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
    if (exactPath) {
      return this.paths.stream().anyMatch(path::equals);
    }
    return this.paths.stream().anyMatch(excludePath -> path.contains("/" + excludePath));
  }
}
