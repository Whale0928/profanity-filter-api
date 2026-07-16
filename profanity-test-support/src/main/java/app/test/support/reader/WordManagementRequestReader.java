package app.test.support.reader;

import app.test.support.fixture.SeedApiKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;

public final class WordManagementRequestReader {

  private final DataSource dataSource;

  public WordManagementRequestReader(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public int countRequests(SeedApiKey client, String word, String requestType) {
    String sql =
        """
        SELECT COUNT(*)
        FROM word_management
        WHERE request_user_id = UNHEX(REPLACE(?, '-', ''))
          AND word = ?
          AND request_type = ?
        """;

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, client.id());
      statement.setString(2, word);
      statement.setString(3, requestType);

      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to inspect word_management table", e);
    }
  }
}
