package app.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.test.support.container.MySqlTestContainer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.mysql.MySQLContainer;

class AdminPortalMigrationTest {
  private static final String OLD_WORD_REQUESTS =
      "SELECT id, HEX(request_user_id), word, reason, severity, request_type, status, requested_at FROM word_management ORDER BY id";
  private static final String OLD_USERS =
      "SELECT HEX(id), display_name, primary_email, status, created_at, updated_at FROM users ORDER BY id";
  private static final String OLD_KEYS =
      "SELECT HEX(id), HEX(user_id), name, key_hash, key_hint, permissions, issued_at, expired_at, request_count FROM api_keys ORDER BY id";

  @Test
  @DisplayName("V5는 V4 원본 데이터를 보존하고 요청자를 API Key 관계로만 연결한다")
  void migrateV4_preservesDataAndBackfillsOnlyKnownOwnership() throws Exception {
    try (MySQLContainer container = MySqlTestContainer.create()) {
      container.start();
      flyway(container, "4").migrate();
      List<List<String>> requestsBefore;
      List<List<String>> usersBefore;
      List<List<String>> keysBefore;
      try (Connection connection = connection(container)) {
        seedV4(connection);
        requestsBefore = rows(connection, OLD_WORD_REQUESTS);
        usersBefore = rows(connection, OLD_USERS);
        keysBefore = rows(connection, OLD_KEYS);
      }

      var migrated = flyway(container, null).migrate();
      assertThat(migrated.migrationsExecuted).isEqualTo(1);
      assertThat(flyway(container, null).migrate().migrationsExecuted).isZero();
      assertThat(flyway(container, null).validateWithResult().validationSuccessful).isTrue();

      try (Connection connection = connection(container)) {
        assertThat(rows(connection, OLD_WORD_REQUESTS)).isEqualTo(requestsBefore);
        assertThat(rows(connection, OLD_USERS)).isEqualTo(usersBefore);
        assertThat(rows(connection, OLD_KEYS)).isEqualTo(keysBefore);
        assertThat(rows(connection, "SELECT DISTINCT role FROM users"))
            .containsExactly(List.of("CLIENT"));
        assertThat(
                rows(
                    connection,
                    "SELECT COUNT(*) FROM users WHERE role = 'ADMIN' OR last_login_at IS NOT NULL"))
            .containsExactly(List.of("0"));
        assertThat(
                rows(
                    connection, "SELECT id, word, is_used, source FROM profanity_word ORDER BY id"))
            .containsExactly(List.of("1", "기존 표현", "N", "UNKNOWN"));
        assertThat(
                rows(
                    connection,
                    "SELECT COUNT(*) FROM profanity_word WHERE created_at IS NOT NULL OR updated_at IS NOT NULL OR created_by IS NOT NULL OR updated_by IS NOT NULL"))
            .containsExactly(List.of("0"));
        assertThat(
                rows(
                    connection,
                    "SELECT COUNT(*) FROM api_keys WHERE last_used_at IS NOT NULL OR revoked_by IS NOT NULL OR revocation_reason IS NOT NULL"))
            .containsExactly(List.of("0"));
        assertThat(rows(connection, "SELECT COUNT(*) FROM inquiries"))
            .containsExactly(List.of("3"));
        assertThat(rows(connection, "SELECT id, inquiry_id FROM word_management ORDER BY id"))
            .containsExactly(List.of("7", "7"), List.of("42", "42"), List.of("99", "99"));
        assertThat(
                rows(
                    connection,
                    "SELECT requester_user_id = UNHEX(LPAD('11',32,'0')), requester_api_key_id = UNHEX(LPAD('22',32,'0')) FROM inquiries WHERE id=7"))
            .containsExactly(List.of("1", "1"));
        assertThat(
                rows(
                    connection,
                    "SELECT requester_user_id IS NULL, requester_api_key_id = UNHEX(LPAD('33',32,'0')) FROM inquiries WHERE id=42"))
            .containsExactly(List.of("1", "1"));
        // The orphan's identifier deliberately equals users.id: never infer identity from that.
        assertThat(
                rows(
                    connection,
                    "SELECT requester_user_id IS NULL, requester_api_key_id IS NULL FROM inquiries WHERE id=99"))
            .containsExactly(List.of("1", "1"));
        assertThat(
                rows(
                    connection,
                    "SELECT COUNT(*) FROM inquiries i JOIN word_management w ON i.id=w.inquiry_id WHERE i.content=w.reason AND i.created_at=TIMESTAMPADD(HOUR,-9,w.requested_at) AND i.updated_at=TIMESTAMPADD(HOUR,-9,w.requested_at) AND i.resolved_at IS NULL"))
            .containsExactly(List.of("3"));
        assertThat(rows(connection, "SELECT CHAR_LENGTH(title) FROM inquiries WHERE id=99"))
            .containsExactly(List.of("160"));
        execute(
            connection,
            "INSERT INTO inquiries(type,title,content,status,created_at,updated_at) VALUES ('GENERAL','new','new','RECEIVED',CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))");
        assertThat(rows(connection, "SELECT MAX(id) > 99 FROM inquiries"))
            .containsExactly(List.of("1"));
        assertThatThrownBy(() -> execute(connection, "UPDATE users SET role='USER'"))
            .isInstanceOf(SQLException.class);
        assertThatThrownBy(
                () ->
                    execute(connection, "UPDATE word_management SET inquiry_id=999999 WHERE id=7"))
            .isInstanceOf(SQLException.class);
        assertThat(rows(connection, "SELECT COUNT(*) FROM news_posts"))
            .containsExactly(List.of("0"));
        assertThat(rows(connection, "SELECT COUNT(*) FROM admin_audit_logs"))
            .containsExactly(List.of("0"));
      }
    }
  }

  @Test
  @DisplayName("빈 DB에도 V1부터 V5까지 순서대로 적용되고 재실행은 변경을 만들지 않는다")
  void migrateFreshDatabase_appliesAllFiveOnce() throws Exception {
    try (MySQLContainer container = MySqlTestContainer.create()) {
      container.start();
      assertThat(flyway(container, null).migrate().migrationsExecuted).isEqualTo(5);
      assertThat(flyway(container, null).migrate().migrationsExecuted).isZero();
      assertThat(flyway(container, null).validateWithResult().validationSuccessful).isTrue();
      try (Connection connection = connection(container)) {
        assertThat(rows(connection, "SELECT COUNT(*) FROM inquiries"))
            .containsExactly(List.of("0"));
        execute(
            connection,
            "INSERT INTO users(id,display_name,primary_email,status,created_at,updated_at) VALUES (UNHEX(LPAD('55',32,'0')),'new','new@example.test','ACTIVE',CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))");
        assertThat(rows(connection, "SELECT role FROM users")).containsExactly(List.of("CLIENT"));
      }
    }
  }

  private void seedV4(Connection connection) throws Exception {
    execute(
        connection,
        """
        INSERT INTO users(id,display_name,primary_email,status,created_at,updated_at)
        VALUES (UNHEX(LPAD('11',32,'0')),'기존 사용자','legacy@example.test','DISABLED','2026-01-02 03:04:05','2026-02-03 04:05:06')
        """);
    execute(
        connection,
        """
        INSERT INTO api_keys(id,user_id,name,email,key_hash,key_hint,issuer_info,permissions,issued_at,expired_at,request_count)
        VALUES (UNHEX(LPAD('22',32,'0')),UNHEX(LPAD('11',32,'0')),'owned','legacy@example.test',REPEAT('a',64),'example only','test','READ','2026-01-02',NULL,13),
               (UNHEX(LPAD('33',32,'0')),NULL,'unowned','unowned@example.test',REPEAT('b',64),'example only','test','READ/WRITE','2026-01-02','2026-01-03',27)
        """);
    execute(connection, "INSERT INTO profanity_word(id,word,is_used) VALUES (1,'기존 표현','N')");
    execute(
        connection,
        """
        INSERT INTO word_management(id,request_user_id,word,reason,severity,request_type,status,requested_at)
        VALUES (7,UNHEX(LPAD('22',32,'0')),'표현 A','원래 사유 A','LOW','NEW','REQUEST','2026-03-01 01:02:03'),
               (42,UNHEX(LPAD('33',32,'0')),'표현 B','원래 사유 B','HIGH','EXCEPTION','REQUEST','2026-03-02 01:02:03'),
               (99,UNHEX(LPAD('11',32,'0')),REPEAT('가',200),'소유 관계가 없는 과거 기록','NORMAL','MODIFY','LEGACY_REVIEW','2026-03-03 01:02:03')
        """);
  }

  private Flyway flyway(MySQLContainer container, String target) {
    var config =
        Flyway.configure()
            .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
            .locations("classpath:db/migration");
    if (target != null) config.target(target);
    return config.load();
  }

  private Connection connection(MySQLContainer container) throws SQLException {
    return DriverManager.getConnection(
        container.getJdbcUrl(), container.getUsername(), container.getPassword());
  }

  private void execute(Connection connection, String sql) throws SQLException {
    try (var statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    }
  }

  private List<List<String>> rows(Connection connection, String sql) throws SQLException {
    List<List<String>> rows = new ArrayList<>();
    try (var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      while (result.next()) {
        List<String> row = new ArrayList<>();
        for (int i = 1; i <= result.getMetaData().getColumnCount(); i++)
          row.add(result.getString(i));
        rows.add(row);
      }
    }
    return rows;
  }
}
