package app.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "redis.cluster")
public record RedisProperties(
        List<String> master,
        List<String> replica,
        String password
) {
}
