package app.security.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpMethod;

import java.util.List;

@Getter
@AllArgsConstructor
public enum ExcludePath {
    ACTUATOR("system/actuator", "시스템 관련 API", List.of(HttpMethod.GET)),
    CLIENTS("clients", "클라이언트 관련 API", List.of(HttpMethod.POST)),
    FILTER("filter", "필터 관련 API", List.of(HttpMethod.GET, HttpMethod.POST)),
    RESOURCE("resource", "리소스 관련 API", List.of(HttpMethod.GET));

    private final String path;
    private final String description;
    private final List<HttpMethod> method;

    public static List<ExcludePath> getPaths() {
        return List.of(ExcludePath.values());
    }

    public static boolean isExcluded(String path) {
        return getPaths().stream()
                .anyMatch(excludePath -> path.contains("/" + excludePath));
    }

    public boolean isPossibleMethod(String method) {
        return String.valueOf(this.getMethod()).equals(method);
    }

    public boolean isMatch(String path, String method) {
        return path.contains("/" + this.getPath()) && !isPossibleMethod(method);
    }
}
