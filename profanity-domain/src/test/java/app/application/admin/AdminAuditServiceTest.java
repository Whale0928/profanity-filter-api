package app.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.domain.InMemoryAdminAuditLogRepository;
import app.domain.audit.AdminAuditLog;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminAuditServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-10T03:00:00Z");
  private static final UUID ACTOR = UUID.randomUUID();

  private final InMemoryAdminAuditLogRepository repository = new InMemoryAdminAuditLogRepository();
  private final AdminAuditService adminAuditService = new AdminAuditService(repository);

  @Test
  @DisplayName("작업자, 행위, 대상, 사유, 시각을 그대로 기록한다")
  void record_storesGivenFields() {
    adminAuditService.record(
        ACTOR, AdminAuditAction.WORD_CREATED, AdminAuditTargetType.WORD, "12", "신규 등록", NOW);

    assertThat(repository.findAll())
        .singleElement()
        .satisfies(
            log -> {
              assertThat(log.getActorUserId()).isEqualTo(ACTOR);
              assertThat(log.getAction()).isEqualTo("WORD_CREATED");
              assertThat(log.getTargetType()).isEqualTo("WORD");
              assertThat(log.getTargetId()).isEqualTo("12");
              assertThat(log.getReason()).isEqualTo("신규 등록");
              assertThat(log.getCreatedAt()).isEqualTo(NOW);
            });
  }

  @Test
  @DisplayName("사유가 비어 있으면 null로 저장한다")
  void record_blankReason_storesNull() {
    adminAuditService.record(
        ACTOR, AdminAuditAction.WORD_CREATED, AdminAuditTargetType.WORD, "12", "   ", NOW);

    assertThat(repository.findAll().get(0).getReason()).isNull();
  }

  @Test
  @DisplayName("긴 사유는 컬럼 길이에 맞춰 잘라 저장한다")
  void record_longReason_isTruncated() {
    String reason = "가".repeat(700);

    adminAuditService.record(
        ACTOR, AdminAuditAction.WORD_CREATED, AdminAuditTargetType.WORD, "12", reason, NOW);

    assertThat(repository.findAll().get(0).getReason()).hasSize(500);
  }

  @Test
  @DisplayName("작업자가 없으면 기록하지 않는다")
  void record_withoutActor_isRejected() {
    assertThatThrownBy(
            () ->
                adminAuditService.record(
                    null,
                    AdminAuditAction.WORD_CREATED,
                    AdminAuditTargetType.WORD,
                    "12",
                    null,
                    NOW))
        .isInstanceOf(NullPointerException.class);
    assertThat(repository.findAll()).isEmpty();
  }

  @Test
  @DisplayName("대상별로 최신 기록부터 조회한다")
  void findAllByTarget_returnsNewestFirst() {
    adminAuditService.record(
        ACTOR, AdminAuditAction.WORD_CREATED, AdminAuditTargetType.WORD, "12", "첫 기록", NOW);
    adminAuditService.record(
        ACTOR,
        AdminAuditAction.WORD_UPDATED,
        AdminAuditTargetType.WORD,
        "12",
        "두 번째 기록",
        NOW.plusSeconds(60));

    assertThat(
            repository.findAllByTargetTypeAndTargetIdOrderByIdDesc(AdminAuditTargetType.WORD, "12"))
        .extracting(AdminAuditLog::getReason)
        .containsExactly("두 번째 기록", "첫 기록");
  }
}
