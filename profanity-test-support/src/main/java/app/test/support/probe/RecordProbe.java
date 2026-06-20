package app.test.support.probe;

import app.test.support.fixture.SeedClient;
import app.test.support.fixture.SeedWord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;

public final class RecordProbe {

  private final DataSource dataSource;

  public RecordProbe(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public void assertFilterRecord(SeedClient client, String requestText, SeedWord word) {
    int count = countFilterRecords(client, requestText, word);

    if (count != 1) {
      throw new AssertionError(
          "Expected exactly one filter record, but found " + count + " for " + requestText);
    }
  }

  public int countFilterRecords(SeedClient client, String requestText, SeedWord word) {
    String sql =
        """
        SELECT COUNT(*)
        FROM records
        WHERE api_key = ?
          AND request_text = ?
          AND mode = 'FILTER'
          AND words = ?
        """;

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, client.apiKey());
      statement.setString(2, requestText);
      statement.setString(3, word.word());

      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to inspect records table", e);
    }
  }
}
