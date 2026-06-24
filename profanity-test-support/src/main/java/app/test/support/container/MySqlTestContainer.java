package app.test.support.container;

import app.test.support.database.DatabaseSeedSupport;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings("resource")
public final class MySqlTestContainer {

  private static final String IMAGE = "mysql:8.4";
  private static final String DATABASE_NAME = "profanity";
  private static final String USERNAME = "test";
  private static final String PASSWORD = "test";

  private MySqlTestContainer() {}

  public static MySQLContainer create() {
    return new MySQLContainer(DockerImageName.parse(IMAGE))
        .withDatabaseName(DATABASE_NAME)
        .withUsername(USERNAME)
        .withPassword(PASSWORD);
  }

  public static void migrateSchema(MySQLContainer container) {
    Flyway.configure()
        .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }

  public static void resetSeedData(MySQLContainer container) {
    DatabaseSeedSupport.executeDefault(dataSource(container));
  }

  public static DataSource dataSource(MySQLContainer container) {
    return new DriverManagerDataSource(
        container.getJdbcUrl(), container.getUsername(), container.getPassword());
  }
}
