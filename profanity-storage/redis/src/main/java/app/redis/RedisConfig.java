package app.redis;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisConfig {
    private final RedisProperties redisProperties;

    public RedisConfig(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
        log.info("redis 설정 : {}", redisProperties);
    }

    @PostConstruct
    public void init() {
        log.info("redis 설정 초기화 완료 :{}", redisProperties);
    }

    //todo : 추후 redis 기능 필요 시 기능 구현
}
