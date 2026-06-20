package app.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.ProfanityFilterApplication;
import app.core.data.response.FilterApiResponse;
import app.test.support.container.MySqlTestContainer;
import app.test.support.fixture.IntegrationClient;
import app.test.support.fixture.IntegrationWord;
import app.test.support.probe.RecordProbe;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.mysql.MySQLContainer;

@SuppressWarnings("resource")
@SpringBootTest(
    classes = ProfanityFilterApplication.class,
    properties = {
      "spring.mail.username=e2e@example.com",
      "spring.mail.password=e2e-password",
      "redis.main.password="
    })
@AutoConfigureMockMvc
abstract class AbstractApiTester {

  private static final MySQLContainer MYSQL = MySqlTestContainer.create();

  @Autowired protected MockMvcTester mockMvcTester;

  @Autowired protected ObjectMapper objectMapper;

  @Autowired private DataSource dataSource;

  @Autowired private CacheManager cacheManager;

  private RecordProbe recordProbe;

  @DynamicPropertySource
  static void configureDatabase(DynamicPropertyRegistry registry) {
    MYSQL.start();
    MySqlTestContainer.migrateSchema(MYSQL);
    MySqlTestContainer.resetSeedData(MYSQL);

    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
  }

  @AfterAll
  static void stopContainer() {
    MYSQL.stop();
  }

  @BeforeEach
  void setUpApiTester() {
    MySqlTestContainer.resetSeedData(MYSQL);
    cacheManager
        .getCacheNames()
        .forEach(
            name -> {
              var cache = cacheManager.getCache(name);
              if (cache != null) {
                cache.clear();
              }
            });

    recordProbe = new RecordProbe(dataSource);
  }

  protected FilterResponseAssertion assertFilterResponse(FilterApiResponse response) {
    return new FilterResponseAssertion(response);
  }

  protected RecordAssertion assertRecord(IntegrationClient client) {
    return new RecordAssertion(recordProbe, client);
  }

  protected static final class FilterResponseAssertion {
    private final FilterApiResponse response;

    private FilterResponseAssertion(FilterApiResponse response) {
      this.response = response;
    }

    FilterResponseAssertion hasDetected(IntegrationWord word) {
      assertNotNull(response.trackingId());
      assertEquals(2000, response.status().code());
      assertTrue(
          response.detected().stream()
              .anyMatch(detected -> detected.filteredWord().equals(word.word())));
      return this;
    }

    FilterResponseAssertion hasFilteredText(String filteredText) {
      assertEquals(filteredText, response.filtered());
      return this;
    }
  }

  protected static final class RecordAssertion {
    private final RecordProbe recordProbe;
    private final IntegrationClient client;

    private RecordAssertion(RecordProbe recordProbe, IntegrationClient client) {
      this.recordProbe = recordProbe;
      this.client = client;
    }

    void hasFilterRecord(String requestText, IntegrationWord word) {
      recordProbe.assertFilterRecord(client, requestText, word);
    }
  }
}
