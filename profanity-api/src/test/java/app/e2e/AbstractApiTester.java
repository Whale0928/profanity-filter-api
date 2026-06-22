package app.e2e;

import app.ProfanityFilterApplication;
import app.test.support.container.MySqlTestContainer;
import app.test.support.probe.RecordProbe;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.mysql.MySQLContainer;

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

  protected RecordProbe recordProbe;

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
}
