package app.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.application.admin.AdminApiKeyService.AdminApiKeyView;
import app.application.admin.AdminApiKeyService.ApiKeyStatus;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.InMemoryAdminAuditLogRepository;
import app.domain.InMemoryApiKeyRepository;
import app.domain.InMemoryUserAccountRepository;
import app.domain.apikey.ApiKey;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import app.domain.user.UserAccount;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminApiKeyServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-10T03:00:00Z");
  private static final LocalDateTime ISSUED_AT = LocalDateTime.of(2026, 9, 1, 10, 0);

  private final InMemoryApiKeyRepository apiKeyRepository = new InMemoryApiKeyRepository();
  private final InMemoryUserAccountRepository userAccountRepository =
      new InMemoryUserAccountRepository();
  private final InMemoryAdminAuditLogRepository auditLogRepository =
      new InMemoryAdminAuditLogRepository();
  private final AdminApiKeyService adminApiKeyService =
      new AdminApiKeyService(
          apiKeyRepository,
          userAccountRepository,
          new AdminAuditService(auditLogRepository),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Nested
  @DisplayName("API Key 목록 조회는")
  class Search {

    @Test
    @DisplayName("소유자 표시 이름을 채우고 키 원문과 해시를 담지 않는다")
    void search_returnsOwnerNameWithoutKeyMaterial() {
      UserAccount owner = saveUser("김소유", "owner@example.test");
      ApiKey apiKey = saveKey(owner.getId(), "운영 서버", "owner@example.test", "hash-1");

      PageResult<AdminApiKeyView> result =
          adminApiKeyService.search(null, null, PageQuery.of(0, 20));

      assertThat(result.items())
          .singleElement()
          .satisfies(
              view -> {
                assertThat(view.id()).isEqualTo(apiKey.getId());
                assertThat(view.ownerName()).isEqualTo("김소유");
                assertThat(view.keyHint()).isEqualTo("hint");
                assertThat(view.active()).isTrue();
                assertThat(view.lastUsedAt()).isNull();
              });
    }

    @Test
    @DisplayName("소유자가 없는 과거 키는 소유자 이름을 비운다")
    void search_unownedKey_hasNullOwnerName() {
      saveKey(null, "레거시 키", "legacy@example.test", "hash-legacy");

      assertThat(adminApiKeyService.search(null, null, PageQuery.of(0, 20)).items())
          .singleElement()
          .satisfies(view -> assertThat(view.ownerName()).isNull());
    }

    @Test
    @DisplayName("상태 필터로 유효한 키와 만료된 키를 나눈다")
    void search_withStatus_filtersByActiveState() {
      saveKey(null, "유효 키", "active@example.test", "hash-active");
      ApiKey expired = saveKey(null, "만료 키", "expired@example.test", "hash-expired");
      expired.expire(ISSUED_AT.plusDays(1));

      PageResult<AdminApiKeyView> active =
          adminApiKeyService.search(null, ApiKeyStatus.ACTIVE, PageQuery.of(0, 20));
      PageResult<AdminApiKeyView> expiredOnly =
          adminApiKeyService.search(null, ApiKeyStatus.EXPIRED, PageQuery.of(0, 20));

      assertThat(active.items()).extracting(AdminApiKeyView::name).containsExactly("유효 키");
      assertThat(expiredOnly.items()).extracting(AdminApiKeyView::name).containsExactly("만료 키");
    }

    @Test
    @DisplayName("이름과 이메일 부분 일치로 검색한다")
    void search_withQuery_matchesNameAndEmail() {
      saveKey(null, "운영 서버", "ops@example.test", "hash-ops");
      saveKey(null, "개발 서버", "dev@example.test", "hash-dev");

      assertThat(adminApiKeyService.search("운영", null, PageQuery.of(0, 20)).items())
          .extracting(AdminApiKeyView::name)
          .containsExactly("운영 서버");
      assertThat(adminApiKeyService.search("dev@", null, PageQuery.of(0, 20)).items())
          .extracting(AdminApiKeyView::name)
          .containsExactly("개발 서버");
    }
  }

  @Nested
  @DisplayName("API Key 폐기는")
  class Revoke {

    @Test
    @DisplayName("만료 시각과 폐기자, 사유를 남기고 감사 기록을 만든다")
    void revoke_activeKey_recordsRevocation() {
      UUID actor = saveUser("이관리", "manager@example.test").getId();
      ApiKey apiKey = saveKey(null, "폐기 대상", "target@example.test", "hash-target");

      AdminApiKeyView revoked = adminApiKeyService.revoke(actor, apiKey.getId(), "사용 종료");

      assertThat(revoked.active()).isFalse();
      assertThat(revoked.expiredAt()).isNotNull();
      assertThat(apiKey.getRevokedBy()).isEqualTo(actor);
      assertThat(apiKey.getRevocationReason()).isEqualTo("사용 종료");
      assertThat(auditLogRepository.findAll())
          .singleElement()
          .satisfies(
              log -> {
                assertThat(log.getAction()).isEqualTo(AdminAuditAction.API_KEY_REVOKED);
                assertThat(log.getTargetType()).isEqualTo(AdminAuditTargetType.API_KEY);
                assertThat(log.getTargetId()).isEqualTo(apiKey.getId().toString());
              });
    }

    @Test
    @DisplayName("이미 만료된 키는 다시 폐기하지 않는다")
    void revoke_expiredKey_isRejected() {
      UUID actor = saveUser("이관리", "manager@example.test").getId();
      ApiKey apiKey = saveKey(null, "폐기 대상", "target@example.test", "hash-target");
      apiKey.expire(ISSUED_AT.plusDays(1));

      assertThatThrownBy(() -> adminApiKeyService.revoke(actor, apiKey.getId(), "중복 폐기"))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.API_KEY_ALREADY_REVOKED.code());
      assertThat(apiKey.getRevokedBy()).isNull();
      assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("없는 키는 API_KEY_NOT_FOUND로 처리한다")
    void revoke_missingKey_throwsNotFound() {
      UUID actor = saveUser("이관리", "manager@example.test").getId();

      assertThatThrownBy(() -> adminApiKeyService.revoke(actor, UUID.randomUUID(), "사유"))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.API_KEY_NOT_FOUND.code());
    }
  }

  private UserAccount saveUser(String displayName, String email) {
    return userAccountRepository.save(UserAccount.create(displayName, email, null, NOW));
  }

  private ApiKey saveKey(UUID userId, String name, String email, String keyHash) {
    ApiKey apiKey =
        ApiKey.issue(
            userId == null ? UUID.randomUUID() : userId,
            name,
            email,
            keyHash,
            "hint",
            "test",
            null,
            ISSUED_AT);
    if (userId == null) {
      // V4 이전에 발급되어 소유자가 연결되지 않은 키를 재현한다.
      clearUserId(apiKey);
    }
    return apiKeyRepository.save(apiKey);
  }

  private static void clearUserId(ApiKey apiKey) {
    try {
      Field field = ApiKey.class.getDeclaredField("userId");
      field.setAccessible(true);
      field.set(apiKey, null);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
