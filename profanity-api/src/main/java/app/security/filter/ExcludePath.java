package app.security.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum ExcludePath {
    CLIENTS("clients", "클라이언트 관련 API"),
    RESOURCE("resource ", "리소스 관련 API");

    private final String path;
    private final String description;

    public static List<String> getPaths() {
        return Stream.of(ExcludePath.values())
                .map(ExcludePath::getPath)
                .toList();
    }

    public static boolean isExcluded(String path) {
        return getPaths().stream()
                .anyMatch(excludePath -> path.startsWith("/" + excludePath));
    }
}
