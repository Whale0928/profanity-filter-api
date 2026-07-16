package app.test.support.probe;

import app.core.data.constant.Mode;
import app.test.support.fixture.SeedApiKey;
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

  public void assertFilterRecord(SeedApiKey client, String requestText, SeedWord word) {
    int count = countFilterRecords(client, requestText, word);

    if (count != 1) {
      throw new AssertionError(
          "Expected exactly one filter record, but found " + count + " for " + requestText);
    }
  }

  public int countFilterRecords(SeedApiKey client, String requestText, SeedWord word) {
    return countRecords(client, requestText, Mode.FILTER, word.word());
  }

  public int countRecords(SeedApiKey client, String requestText, Mode mode, String words) {
    String sql =
        """
        SELECT COUNT(*)
        FROM records
        WHERE api_key_hash = SHA2(?, 256)
          AND request_text = ?
          AND mode = ?
          AND words = ?
        """;

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, client.apiKey());
      statement.setString(2, requestText);
      statement.setString(3, mode.name());
      statement.setString(4, words);

      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to inspect records table", e);
    }
  }

  public int countRecords(SeedApiKey client, String requestText, Mode mode) {
    String sql =
        """
        SELECT COUNT(*)
        FROM records
        WHERE api_key_hash = SHA2(?, 256)
          AND request_text = ?
          AND mode = ?
        """;

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, client.apiKey());
      statement.setString(2, requestText);
      statement.setString(3, mode.name());

      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to inspect records table", e);
    }
  }
}
