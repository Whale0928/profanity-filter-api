package app.application.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.application.admin.AdminAuditAction;
import app.application.admin.AdminAuditService;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.InMemoryAdminAuditLogRepository;
import app.domain.InMemoryNewsPostRepository;
import app.domain.audit.AdminAuditLog;
import app.domain.support.PageQuery;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("관리자 소식 관리")
class AdminNewsServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-10T00:00:00Z");
  private static final UUID ADMIN_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

  private InMemoryNewsPostRepository repository;
  private InMemoryAdminAuditLogRepository auditLogRepository;
  private AdminNewsService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryNewsPostRepository();
    auditLogRepository = new InMemoryAdminAuditLogRepository();
    service =
        new AdminNewsService(
            repository,
            new AdminAuditService(auditLogRepository),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Nested
  @DisplayName("소식을 작성하면")
  class CreateTest {

    @Test
    @DisplayName("게시 상태는 게시 시각을 함께 남긴다")
    void publishedKeepsPublishedAt() {
      var created = service.create(ADMIN_ID, command("공지", "PUBLISHED"));

      assertThat(created.status()).isEqualTo("PUBLISHED");
      assertThat(created.publishedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("임시 저장 상태는 게시 시각을 남기지 않는다")
    void draftHasNoPublishedAt() {
      var created = service.create(ADMIN_ID, command("초안", "DRAFT"));

      assertThat(created.publishedAt()).isNull();
    }

    @Test
    @DisplayName("감사 기록을 같은 작업에서 남긴다")
    void recordsAudit() {
      var created = service.create(ADMIN_ID, command("공지", "PUBLISHED"));

      assertThat(auditLogRepository.findAll())
          .singleElement()
          .satisfies(
              log -> {
                assertThat(log.getAction()).isEqualTo(AdminAuditAction.NEWS_CREATED);
                assertThat(log.getTargetId()).isEqualTo(String.valueOf(created.id()));
                assertThat(log.getActorUserId()).isEqualTo(ADMIN_ID);
              });
    }

    @Test
    @DisplayName("정의되지 않은 분류는 거절한다")
    void rejectsUnknownCategory() {
      assertThatThrownBy(() -> NewsWriteCommand.of("공지", "UNKNOWN", "본문", "DRAFT"))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Nested
  @DisplayName("소식을 수정하면")
  class UpdateTest {

    @Test
    @DisplayName("게시한 소식을 임시 저장으로 되돌리면 게시 시각도 지운다")
    void unpublishClearsPublishedAt() {
      var created = service.create(ADMIN_ID, command("공지", "PUBLISHED"));

      var updated = service.update(ADMIN_ID, created.id(), command("공지", "DRAFT"));

      assertThat(updated.status()).isEqualTo("DRAFT");
      assertThat(updated.publishedAt()).isNull();
    }

    @Test
    @DisplayName("비공개로 되돌린 소식은 공개 목록에서 사라진다")
    void unpublishHidesFromPublicList() {
      var created = service.create(ADMIN_ID, command("공지", "PUBLISHED"));
      service.update(ADMIN_ID, created.id(), command("공지", "DRAFT"));

      var published = repository.searchPublished(null, null, 0, 20);

      assertThat(published.items()).isEmpty();
    }

    @Test
    @DisplayName("없는 소식은 찾을 수 없다고 응답한다")
    void missingPostIsNotFound() {
      assertThatThrownBy(() -> service.update(ADMIN_ID, 404L, command("공지", "DRAFT")))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.NEWS_NOT_FOUND.code());
    }
  }

  @Nested
  @DisplayName("소식 목록과 삭제는")
  class ListAndDeleteTest {

    @Test
    @DisplayName("임시 저장 소식도 본문과 함께 보여 준다")
    void adminListIncludesDraftContent() {
      service.create(ADMIN_ID, command("초안", "DRAFT"));

      var result = service.search(null, null, null, PageQuery.of(null, null));

      assertThat(result.items())
          .singleElement()
          .satisfies(
              item -> {
                assertThat(item.status()).isEqualTo("DRAFT");
                assertThat(item.content()).isEqualTo("본문");
              });
    }

    @Test
    @DisplayName("삭제하면 목록에서 사라지고 감사 기록을 남긴다")
    void deleteRemovesAndRecords() {
      var created = service.create(ADMIN_ID, command("공지", "PUBLISHED"));

      service.delete(ADMIN_ID, created.id());

      assertThat(service.search(null, null, null, PageQuery.of(null, null)).items()).isEmpty();
      assertThat(auditLogRepository.findAll())
          .extracting(AdminAuditLog::getAction)
          .contains(AdminAuditAction.NEWS_DELETED);
    }
  }

  private static NewsWriteCommand command(String title, String status) {
    return NewsWriteCommand.of(title, "NOTICE", "본문", status);
  }
}
