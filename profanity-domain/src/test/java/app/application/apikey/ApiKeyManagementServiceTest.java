package app.application.apikey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.application.apikey.ApiKeyManagementService.CreateApiKeyCommand;
import app.application.client.APIKeyGenerator;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.InMemoryApiKeyRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiKeyManagementServiceTest {
  private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");
  private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID OTHER_USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

  private InMemoryApiKeyRepository repository;
  private APIKeyGenerator keyGenerator;
  private ApiKeyManagementService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryApiKeyRepository();
    keyGenerator = new APIKeyGenerator("test-salt", "SHA-256");
    service =
        new ApiKeyManagementService(repository, keyGenerator, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("발급 이메일은 로그인 사용자의 이메일로 고정하고 원문은 저장하지 않는다")
  void issue_loginEmail_returnsPlaintextOnceWithoutStoringIt() {
    var issued =
        service.issue(
            USER_ID,
            "User@Example.com",
            new CreateApiKeyCommand("운영", "production server", "primary"));

    var stored = repository.findAll().get(0);
    assertThat(issued.key().email()).isEqualTo("user@example.com");
    assertThat(issued.apiKey()).isNotBlank();
    assertThat(stored.getKeyHash()).isEqualTo(keyGenerator.hashApiKey(issued.apiKey()));
    assertThat(stored.getKeyHash()).isNotEqualTo(issued.apiKey());
  }

  @Test
  @DisplayName("한 사용자는 여러 활성 API Key를 발급할 수 있다")
  void issue_twice_keepsMultipleActiveKeys() {
    issue("개발");
    issue("운영");

    assertThat(service.findAll(USER_ID)).hasSize(2).allMatch(key -> key.status().equals("ACTIVE"));
  }

  @Test
  @DisplayName("재발행은 기존 키를 만료하고 새 원문을 한 번 반환한다")
  void reissue_activeKey_expiresPreviousAndCreatesReplacement() {
    var original = issue("운영");

    var replacement = service.reissue(USER_ID, original.key().id());

    assertThat(replacement.apiKey()).isNotEqualTo(original.apiKey());
    assertThat(service.findAll(USER_ID))
        .extracting(ApiKeyManagementService.ApiKeyView::status)
        .containsExactlyInAnyOrder("ACTIVE", "EXPIRED");
  }

  @Test
  @DisplayName("만료 요청은 반복해도 만료 시각을 변경하지 않는다")
  void expire_twice_isIdempotent() {
    var issued = issue("운영");

    var first = service.expire(USER_ID, issued.key().id());
    var second = service.expire(USER_ID, issued.key().id());

    assertThat(second.expiredAt()).isEqualTo(first.expiredAt());
    assertThat(second.status()).isEqualTo("EXPIRED");
  }

  @Test
  @DisplayName("다른 사용자의 API Key는 존재하지 않는 것처럼 처리한다")
  void expire_otherUsersKey_returnsNotFound() {
    var issued = issue("운영");

    assertThatThrownBy(() -> service.expire(OTHER_USER_ID, issued.key().id()))
        .isInstanceOf(BusinessException.class)
        .extracting("status.code")
        .isEqualTo(StatusCode.API_KEY_NOT_FOUND.code());
  }

  private ApiKeyManagementService.IssuedApiKey issue(String name) {
    return service.issue(
        USER_ID, "user@example.com", new CreateApiKeyCommand(name, name + " server", null));
  }
}
