package app.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;


@ConfigurationProperties(prefix = "redis")
public record RedisProperties(
        String host,
        Main main,
        Sub sub
) {
    public RedisProperties {
        if (host == null || host.isBlank())
            throw new IllegalArgumentException("Redis 호스트 설정은 필수입니다");

        if (main == null || main.port <= 0)
            throw new IllegalArgumentException("메인 노드의 포트 설정이 올바르지 않습니다");
        if (sub == null || sub.port == null || sub.port.isEmpty())
            throw new IllegalArgumentException("서브 노드의 포트 설정은 필수입니다");

        sub.port.forEach(port -> {
            if (port <= 0) {
                throw new IllegalArgumentException("서브 노드의 포트는 0보다 커야 합니다");
            }
        });
    }

    public record Main(
            int port
    ) {
    }

    public record Sub(
            List<Integer> port
    ) {
    }
}
