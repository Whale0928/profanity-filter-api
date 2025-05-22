package app.redis;

import io.lettuce.core.ReadFrom;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisStaticMasterReplicaConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
@Slf4j
@EnableRedisRepositories
@RequiredArgsConstructor
public class RedisConfig {
    private final RedisProperties redisProperties;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        if (redisProperties.isSingleMode()) {
            // 단일 Redis 모드
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisProperties.host());
            config.setPort(redisProperties.main().port());

            // 패스워드 설정
            if (redisProperties.main().password() != null && !redisProperties.main().password().isBlank()) {
                config.setPassword(redisProperties.main().password());
            }

            LettuceClientConfiguration lettuceClient = LettuceClientConfiguration.builder().build();
            return new LettuceConnectionFactory(config, lettuceClient);

        } else {
            // 복제 Redis 모드 (기존 로직 유지)
            LettuceClientConfiguration lettuceClient = LettuceClientConfiguration.builder()
                    .readFrom(ReadFrom.REPLICA_PREFERRED)
                    .build();

            RedisStaticMasterReplicaConfiguration config =
                    new RedisStaticMasterReplicaConfiguration(redisProperties.host(), redisProperties.main().port());

            // 패스워드 설정
            if (redisProperties.main().password() != null && !redisProperties.main().password().isBlank()) {
                config.setPassword(redisProperties.main().password());
            }

            // 복제 노드들 추가
            redisProperties.sub().port().forEach(port ->
                    config.addNode(redisProperties.host(), port));

            return new LettuceConnectionFactory(config, lettuceClient);
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // 직렬화 설정
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setStringSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @PostConstruct
    public void init() {
        log.info("Redis 연결 정보");
        log.info("Host: {}", redisProperties.host());
        log.info("Main Port: {}", redisProperties.main().port());
        if (!redisProperties.isSingleMode()) {
            log.info("Sub Ports: {}", redisProperties.sub().port());
        } else {
            log.info("Single Redis Mode");
        }
    }
}
