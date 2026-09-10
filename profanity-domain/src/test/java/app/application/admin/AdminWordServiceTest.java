package app.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.application.admin.AdminWordService.AdminWordView;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.InMemoryAdminAuditLogRepository;
import app.domain.InmemoryProfanityRepository;
import app.domain.profanity.ProfanityWord;
import app.domain.profanity.constant.WordSource;
import app.domain.profanity.constant.isUsedType;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminWordServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-10T03:00:00Z");
  private static final UUID ACTOR = UUID.randomUUID();

  private final InmemoryProfanityRepository profanityRepository = new InmemoryProfanityRepository();
  private final InMemoryAdminAuditLogRepository auditLogRepository =
      new InMemoryAdminAuditLogRepository();
  private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
  private final AdminWordService adminWordService =
      new AdminWordService(
          profanityRepository,
          new AdminAuditService(auditLogRepository),
          eventPublisher,
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Nested
  @DisplayName("사전 단어 등록은")
  class Create {

    @Test
    @DisplayName("출처를 ADMIN으로 남기고 사전 변경 이벤트를 발행한다")
    void create_newWord_marksAdminSourceAndPublishesChange() {
      AdminWordView created = adminWordService.create(ACTOR, "  등록할표현  ");

      assertThat(created.word()).isEqualTo("등록할표현");
      assertThat(created.source()).isEqualTo(WordSource.ADMIN.name());
      assertThat(created.isUsed()).isEqualTo(isUsedType.Y.name());
      assertThat(created.createdAt()).isEqualTo(NOW);
      assertThat(eventPublisher.eventsOf(ProfanityDictionaryChangedEvent.class)).hasSize(1);
      assertThat(auditLogRepository.findAll())
          .singleElement()
          .satisfies(
              log -> {
                assertThat(log.getAction()).isEqualTo(AdminAuditAction.WORD_CREATED);
                assertThat(log.getTargetType()).isEqualTo(AdminAuditTargetType.WORD);
                assertThat(log.getReason()).isEqualTo("등록할표현");
              });
    }

    @Test
    @DisplayName("이미 있는 단어는 WORD_ALREADY_EXISTS로 거절하고 사전을 바꾸지 않는다")
    void create_duplicatedWord_isRejected() {
      adminWordService.create(ACTOR, "중복표현");

      assertThatThrownBy(() -> adminWordService.create(ACTOR, "중복표현"))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.WORD_ALREADY_EXISTS.code());
      assertThat(profanityRepository.countAll()).isEqualTo(1);
      assertThat(eventPublisher.eventsOf(ProfanityDictionaryChangedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("공백 단어는 BAD_REQUEST로 거절한다")
    void create_blankWord_isRejected() {
      assertThatThrownBy(() -> adminWordService.create(ACTOR, "   "))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.BAD_REQUEST.code());
    }
  }

  @Nested
  @DisplayName("사전 단어 수정은")
  class Update {

    @Test
    @DisplayName("표현과 사용 여부를 함께 바꾸고 사전 변경 이벤트를 발행한다")
    void update_changesWordAndUsage() {
      Long id = adminWordService.create(ACTOR, "이전표현").id();
      eventPublisher.events();

      AdminWordView updated = adminWordService.update(ACTOR, id, "새표현", isUsedType.N);

      assertThat(updated.word()).isEqualTo("새표현");
      assertThat(updated.isUsed()).isEqualTo("N");
      assertThat(updated.updatedAt()).isEqualTo(NOW);
      assertThat(profanityRepository.findAllByIsUsed(isUsedType.Y)).isEmpty();
      assertThat(eventPublisher.eventsOf(ProfanityDictionaryChangedEvent.class)).hasSize(2);
      assertThat(auditLogRepository.findAll())
          .extracting(log -> log.getAction())
          .containsExactly(AdminAuditAction.WORD_CREATED, AdminAuditAction.WORD_UPDATED);
    }

    @Test
    @DisplayName("같은 단어를 그대로 저장해도 중복으로 보지 않는다")
    void update_sameWord_isAllowed() {
      Long id = adminWordService.create(ACTOR, "유지표현").id();

      AdminWordView updated = adminWordService.update(ACTOR, id, "유지표현", isUsedType.N);

      assertThat(updated.word()).isEqualTo("유지표현");
      assertThat(updated.isUsed()).isEqualTo("N");
    }

    @Test
    @DisplayName("다른 단어와 표현이 겹치면 거절한다")
    void update_conflictingWord_isRejected() {
      adminWordService.create(ACTOR, "먼저표현");
      Long id = adminWordService.create(ACTOR, "나중표현").id();

      assertThatThrownBy(() -> adminWordService.update(ACTOR, id, "먼저표현", isUsedType.Y))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.WORD_ALREADY_EXISTS.code());
      assertThat(profanityRepository.findById(id).orElseThrow().getWord()).isEqualTo("나중표현");
    }

    @Test
    @DisplayName("없는 단어는 WORD_NOT_FOUND로 처리한다")
    void update_missingWord_throwsWordNotFound() {
      assertThatThrownBy(() -> adminWordService.update(ACTOR, 999L, "표현", isUsedType.Y))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.WORD_NOT_FOUND.code());
    }
  }

  @Nested
  @DisplayName("사전 목록 조회는")
  class Search {

    @Test
    @DisplayName("사용 여부와 검색어로 거른다")
    void search_filtersByUsageAndQuery() {
      profanityRepository.save(ProfanityWord.create("사용중표현", WordSource.BASE, null, NOW));
      ProfanityWord disabled = ProfanityWord.create("중지된표현", WordSource.BASE, null, NOW);
      disabled.changeUsage(isUsedType.N, null, NOW);
      profanityRepository.save(disabled);

      PageResult<AdminWordView> used =
          adminWordService.search(null, isUsedType.Y, PageQuery.of(0, 20));
      PageResult<AdminWordView> byQuery = adminWordService.search("중지", null, PageQuery.of(0, 20));

      assertThat(used.items()).extracting(AdminWordView::word).containsExactly("사용중표현");
      assertThat(byQuery.items()).extracting(AdminWordView::word).containsExactly("중지된표현");
    }

    @Test
    @DisplayName("과거 데이터는 출처가 UNKNOWN이고 시각이 비어 있다")
    void search_legacyRows_reportUnknownSource() {
      profanityRepository.save(new ProfanityWord(null, "과거표현", isUsedType.Y));

      AdminWordView view = adminWordService.search(null, null, PageQuery.of(0, 20)).items().get(0);

      assertThat(view.source()).isEqualTo(WordSource.UNKNOWN.name());
      assertThat(view.createdAt()).isNull();
      assertThat(view.updatedAt()).isNull();
    }
  }
}
