package app.security.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpMethod;

import java.util.List;

@Getter
@AllArgsConstructor
public enum ExcludePath {
    ACTUATOR("actuator", "시스템 관련 API", List.of(HttpMethod.GET)),
    CLIENTS("clients", "클라이언트 관련 API", List.of(HttpMethod.POST)),
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

    /**
     * 가능한 메소드 타입인지 확인
     * 같은 경우 true , 다른 경우 false
     */
    public boolean isPossibleMethod(String method) {
        return this.getMethod().stream()
                .anyMatch(httpMethod -> httpMethod.matches(method));
    }

    public boolean isMatch(String path, String method) {
        boolean contains = path.contains("/" + this.getPath());
        boolean b = isPossibleMethod(method);
        return contains && b;
    }
}
