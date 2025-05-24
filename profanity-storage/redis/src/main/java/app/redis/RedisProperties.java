package app.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "redis")
public record RedisProperties(
        String host,
        Main main,
        Sub sub  // 이제 null 허용
) {
    public RedisProperties {
        if (host == null || host.isBlank())
            throw new IllegalArgumentException("Redis 호스트 설정은 필수입니다");

        if (main == null || main.port <= 0)
            throw new IllegalArgumentException("메인 노드의 포트 설정이 올바르지 않습니다");

        // sub는 선택적으로 변경 - null이거나 빈 포트 목록이어도 허용
        if (sub != null && sub.port != null && !sub.port.isEmpty()) {
            sub.port.forEach(port -> {
                if (port <= 0) {
                    throw new IllegalArgumentException("서브 노드의 포트는 0보다 커야 합니다");
                }
            });
        }
    }

    // 단일 모드인지 확인하는 헬퍼 메서드 추가
    public boolean isSingleMode() {
        return sub == null || sub.port == null || sub.port.isEmpty();
    }

    public record Main(
            int port,
            String password
    ) {
    }

    public record Sub(
            List<Integer> port
    ) {
    }
}
