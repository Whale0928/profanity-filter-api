package app;

import app.application.filter.ProfanityHandler;
import app.application.whitelist.WhitelistResolver;
import app.test.support.fake.FakeProfanityHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {
  @Bean
  @Primary
  public ProfanityHandler fakeProfanityFilterService() {
    return new FakeProfanityHandler();
  }

  /** 컨트롤러 슬라이스 테스트는 허용 단어 그룹을 지정하지 않으므로 그룹 저장소까지 구성하지 않는다. 그룹 동작은 WhitelistE2ETest가 검증한다. */
  @Bean
  public WhitelistResolver whitelistResolver() {
    return new WhitelistResolver(null, null) {
      @Override
      public Set<String> resolve(String apiKeyHash, List<UUID> whitelistIds) {
        return Set.of();
      }
    };
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    // 커스텀 모듈 등록
    SimpleModule customModule = new SimpleModule();
    objectMapper.registerModule(customModule);
    return objectMapper;
  }
}
