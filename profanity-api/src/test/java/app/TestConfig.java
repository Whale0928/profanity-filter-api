package app;

import app.application.client.MetadataReader;
import app.application.filter.ProfanityHandler;
import app.fixture.FakeClientMetadataReader;
import app.fixture.FakeProfanityHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    @Bean
    @Primary
    public MetadataReader fakeMetadataReader() {
        return new FakeClientMetadataReader();
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
