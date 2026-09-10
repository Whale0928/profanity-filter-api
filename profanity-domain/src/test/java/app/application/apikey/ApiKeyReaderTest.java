package app.application.apikey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.application.client.APIKeyGenerator;
import app.domain.InMemoryApiKeyRepository;
import app.domain.apikey.ApiKey;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.CannotCreateTransactionException;

class ApiKeyReaderTest {
  @Test
  @DisplayName("사용 기록의 잠금 및 트랜잭션 실패는 유효한 키 인증을 방해하지 않는다")
  void read_usagePersistenceFailure_preservesAuthentication() throws Exception {
    for (RuntimeException failure :
        List.of(
            new CannotAcquireLockException("test lock failure"),
            new CannotCreateTransactionException("test transaction failure"))) {
      Fixture fixture = fixture(failure);
      assertThat(fixture.reader.read(fixture.key)).isNotNull();
    }
  }

  @Test
  @DisplayName("사용 기록의 프로그래밍 오류는 통계 실패로 숨기지 않는다")
  void read_unexpectedFailure_propagates() throws Exception {
    Fixture fixture = fixture(new IllegalStateException("test programming error"));
    assertThatThrownBy(() -> fixture.reader.read(fixture.key))
        .isInstanceOf(IllegalStateException.class);
  }

  private Fixture fixture(RuntimeException failure) throws Exception {
    var repository = new InMemoryApiKeyRepository();
    var generator = new APIKeyGenerator("test-salt", "SHA-256");
    String key = generator.generateApiKey();
    repository.save(
        ApiKey.issue(
            UUID.randomUUID(),
            "test",
            "test@example.test",
            generator.hashApiKey(key),
            generator.keyHint(key),
            "test",
            null,
            LocalDateTime.now()));
    var recorder =
        new ApiKeyUsageRecorder(repository, Clock.systemUTC()) {
          @Override
          public boolean recordUsage(UUID id) {
            throw failure;
          }
        };
    return new Fixture(new ApiKeyReader(repository, generator, recorder), key);
  }

  private record Fixture(ApiKeyReader reader, String key) {}
}
