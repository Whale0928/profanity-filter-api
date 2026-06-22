package app.test.support.database;

import java.util.Arrays;
import java.util.Comparator;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public final class DatabaseSeedSupport {

  private static final String DEFAULT_SEED_PATTERN = "classpath*:db/seed/*.sql";

  private DatabaseSeedSupport() {}

  public static void executeDefault(DataSource dataSource) {
    executeAll(dataSource, DEFAULT_SEED_PATTERN);
  }

  public static void execute(DataSource dataSource, String resourcePath) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource(resourcePath));
    populator.execute(dataSource);
  }

  public static void executeAll(DataSource dataSource, String resourcePattern) {
    try {
      Resource[] resources =
          new PathMatchingResourcePatternResolver().getResources(resourcePattern);
      Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

      ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
      populator.addScripts(resources);
      populator.execute(dataSource);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to execute seed scripts: " + resourcePattern, e);
    }
  }
}
