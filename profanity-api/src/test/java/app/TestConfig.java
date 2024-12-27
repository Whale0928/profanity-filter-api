package app;

import app.application.FakeApiProfanityFilter;
import app.application.FakeProfanityHandler;
import app.application.ProfanityFilterService;
import app.application.filter.ProfanityHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {
    @Bean
    @Primary
    public ProfanityFilterService fakeProfanityFilter() {
        return new FakeApiProfanityFilter(fakeProfanityFilterService());
    }

    @Bean
    @Primary
    public ProfanityHandler fakeProfanityFilterService() {
        return new FakeProfanityHandler();
    }
}
