package app.test.support.container;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class MySqlTestContainerSmokeTest {

  @Container private static final MySQLContainer MYSQL = MySqlTestContainer.create();

  @Test
  @DisplayName("MySQL 컨테이너에 Flyway migration과 seed scripts를 적용할 수 있다")
  void mysqlContainer_whenMigrateAndSeed_succeeds() throws SQLException {
    MySqlTestContainer.migrateSchema(MYSQL);
    MySqlTestContainer.resetSeedData(MYSQL);

    DataSource dataSource = MySqlTestContainer.dataSource(MYSQL);

    assertThat(count(dataSource, "clients")).isEqualTo(2);
    assertThat(count(dataSource, "profanity_word")).isEqualTo(3);

    execute(
        dataSource,
        """
        insert into clients (id, name, email, api_key, issuer_info, permissions)
        values (UNHEX(REPLACE('00000000-0000-0000-0000-000000000003', '-', '')),
                'Temporary Client',
                'temporary@example.com',
                'temporary-api-key',
                'smoke-test',
                'READ')
        """);

    MySqlTestContainer.resetSeedData(MYSQL);

    assertThat(count(dataSource, "clients")).isEqualTo(2);
  }

  private static int count(DataSource dataSource, String tableName) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("select count(*) from " + tableName)) {
      resultSet.next();
      return resultSet.getInt(1);
    }
  }

  private static void execute(DataSource dataSource, String sql) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
}
