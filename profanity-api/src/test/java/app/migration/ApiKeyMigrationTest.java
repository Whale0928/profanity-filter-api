package app.migration;

import static org.assertj.core.api.Assertions.assertThat;

import app.test.support.container.MySqlTestContainer;
import java.sql.Connection;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.mysql.MySQLContainer;

class ApiKeyMigrationTest {
  private static final String CLIENT_ID = "10000000-0000-0000-0000-000000000001";
  private static final String API_KEY = "legacy-plaintext-api-key";

  @Test
  @DisplayName("V4는 clients와 API Key 원문을 api_keys 해시 모델로 손실 없이 전환한다")
  void migrate_v3ToV4_backfillsKeysAndRemovesLegacyStorage() throws Exception {
    try (MySQLContainer container = MySqlTestContainer.create()) {
      container.start();
      Flyway.configure()
          .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
          .locations("classpath:db/migration")
          .target("3")
          .load()
          .migrate();

      try (Connection connection = connection(container)) {
        insertLegacyData(connection);
      }

      Flyway.configure()
          .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
          .locations("classpath:db/migration")
          .load()
          .migrate();

      try (Connection connection = connection(container)) {
        assertThat(singleString(connection, "SELECT LOWER(HEX(id)) FROM api_keys"))
            .isEqualTo(CLIENT_ID.replace("-", ""));
        assertThat(singleString(connection, "SELECT key_hash FROM api_keys"))
            .isEqualTo(singleString(connection, "SELECT SHA2('" + API_KEY + "', 256)"));
        assertThat(singleString(connection, "SELECT api_key_hash FROM records"))
            .isEqualTo(singleString(connection, "SELECT key_hash FROM api_keys"));
        assertThat(singleLong(connection, tableCountSql("clients"))).isZero();
        assertThat(singleLong(connection, columnCountSql("records", "api_key"))).isZero();
        assertThat(singleLong(connection, columnCountSql("client_reports", "api_key"))).isZero();
        assertThat(singleLong(connection, "SELECT COUNT(*) FROM client_reports")).isEqualTo(1);
      }
    }
  }

  private void insertLegacyData(Connection connection) throws Exception {
    try (var statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          INSERT INTO clients
            (id, name, email, api_key, issuer_info, note, issued_at, permissions, request_count)
          VALUES
            (UNHEX(REPLACE('%s', '-', '')), 'Legacy', 'legacy@example.com', '%s',
             'legacy-system', 'migration-test', CURRENT_TIMESTAMP, 'READ', 1)
          """
              .formatted(CLIENT_ID, API_KEY));
      statement.executeUpdate(
          """
          INSERT INTO records
            (tracking_id, api_key, request_text, mode, words, ip, created_at)
          VALUES
            (UNHEX(REPLACE('20000000-0000-0000-0000-000000000002', '-', '')),
             '%s', 'text', 'QUICK', '', '127.0.0.1', CURRENT_TIMESTAMP(6))
          """
              .formatted(API_KEY));
      statement.executeUpdate(
          """
          INSERT INTO client_reports
            (client_id, api_key, report_year, report_month, report_day, request_count,
             profanity_detection_count, created_at)
          VALUES
            (UNHEX(REPLACE('%s', '-', '')), '%s', 2026, 7, 17, 1, 0, CURRENT_TIMESTAMP)
          """
              .formatted(CLIENT_ID, API_KEY));
    }
  }

  private Connection connection(MySQLContainer container) throws Exception {
    return DriverManager.getConnection(
        container.getJdbcUrl(), container.getUsername(), container.getPassword());
  }

  private String singleString(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      assertThat(result.next()).isTrue();
      return result.getString(1);
    }
  }

  private long singleLong(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      assertThat(result.next()).isTrue();
      return result.getLong(1);
    }
  }

  private String tableCountSql(String table) {
    return "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '"
        + table
        + "'";
  }

  private String columnCountSql(String table, String column) {
    return "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '"
        + table
        + "' AND column_name = '"
        + column
        + "'";
  }
}
