package app.application.apikey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.application.client.APIKeyGenerator;
import app.domain.InMemoryApiKeyRepository;
import app.domain.apikey.ApiKey;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiKeyOwnershipServiceTest {
  private InMemoryApiKeyRepository repository;
  private APIKeyGenerator generator;
  private ApiKeyOwnershipService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryApiKeyRepository();
    generator = new APIKeyGenerator("test-salt", "SHA-256");
    service = new ApiKeyOwnershipService(repository);
  }

  @Test
  @DisplayName("검증된 이메일과 같은 미이관 키를 모두 한 사용자에게 연결한다")
  void claim_matchingUnownedKeys_claimsAllOnce() throws Exception {
    UUID userId = UUID.randomUUID();
    repository.save(unowned("User@Example.com", "first"));
    repository.save(unowned("user@example.com", "second"));
    repository.save(unowned("other@example.com", "other"));

    int first = service.claimUnownedKeys(userId, " USER@example.com ");
    int second = service.claimUnownedKeys(userId, "user@example.com");

    assertThat(first).isEqualTo(2);
    assertThat(second).isZero();
    assertThat(repository.findAllByUserIdOrderByIssuedAtDesc(userId)).hasSize(2);
  }

  @Test
  @DisplayName("이미 다른 사용자에게 연결된 키는 이메일이 같아도 변경하지 않는다")
  void claim_alreadyOwnedKey_preservesOwner() throws Exception {
    UUID originalOwner = UUID.randomUUID();
    ApiKey key = unowned("user@example.com", "owned");
    setUserId(key, originalOwner);
    repository.save(key);

    int claimed = service.claimUnownedKeys(UUID.randomUUID(), "user@example.com");

    assertThat(claimed).isZero();
    assertThat(key.getUserId()).isEqualTo(originalOwner);
  }

  @Test
  @DisplayName("검증된 이메일과 다른 미이관 키는 연결하지 않는다")
  void claim_nonMatchingEmail_preservesUnownedKey() throws Exception {
    ApiKey key = unowned("other@example.com", "other");
    repository.save(key);

    int claimed = service.claimUnownedKeys(UUID.randomUUID(), "user@example.com");

    assertThat(claimed).isZero();
    assertThat(key.getUserId()).isNull();
  }

  @Test
  @DisplayName("사용자 ID나 검증 이메일이 없으면 연결을 시작하지 않는다")
  void claim_missingIdentity_rejectsRequest() {
    UUID userId = UUID.randomUUID();

    assertThatThrownBy(() -> service.claimUnownedKeys(null, "user@example.com"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.claimUnownedKeys(userId, " "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private ApiKey unowned(String email, String name) throws Exception {
    String plaintext = generator.generateApiKey();
    ApiKey key =
        ApiKey.issue(
            UUID.randomUUID(),
            name,
            email,
            generator.hashApiKey(plaintext),
            generator.keyHint(plaintext),
            "legacy",
            null,
            LocalDateTime.of(2026, 7, 17, 9, 0));
    setUserId(key, null);
    return key;
  }

  private void setUserId(ApiKey apiKey, UUID userId) throws Exception {
    Field field = ApiKey.class.getDeclaredField("userId");
    field.setAccessible(true);
    field.set(apiKey, userId);
  }
}
