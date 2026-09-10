package app.application.inquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.application.admin.AdminAuditAction;
import app.application.admin.AdminAuditService;
import app.application.admin.ProfanityDictionaryChangedEvent;
import app.application.admin.RecordingEventPublisher;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.InMemoryAdminAuditLogRepository;
import app.domain.InMemoryApiKeyRepository;
import app.domain.InMemoryInquiryReplyRepository;
import app.domain.InMemoryInquiryRepository;
import app.domain.InMemoryUserAccountRepository;
import app.domain.InMemoryWordManagementRepository;
import app.domain.InmemoryProfanityRepository;
import app.domain.audit.AdminAuditLog;
import app.domain.inquiry.Inquiry;
import app.domain.inquiry.InquiryStatus;
import app.domain.inquiry.InquiryType;
import app.domain.manage.WordManagementRequest;
import app.domain.manage.WordRequestType;
import app.domain.profanity.ProfanityWord;
import app.domain.profanity.constant.WordSource;
import app.domain.profanity.constant.isUsedType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("관리자 문의 처리")
class AdminInquiryServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-10T00:00:00Z");
  private static final UUID ADMIN_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
  private static final UUID REQUESTER_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");

  private InMemoryInquiryRepository inquiryRepository;
  private InMemoryWordManagementRepository wordManagementRepository;
  private InmemoryProfanityRepository profanityRepository;
  private InMemoryAdminAuditLogRepository auditLogRepository;
  private RecordingEventPublisher eventPublisher;
  private AdminInquiryService service;

  @BeforeEach
  void setUp() {
    inquiryRepository = new InMemoryInquiryRepository();
    wordManagementRepository = new InMemoryWordManagementRepository();
    profanityRepository = new InmemoryProfanityRepository();
    auditLogRepository = new InMemoryAdminAuditLogRepository();
    eventPublisher = new RecordingEventPublisher();
    service =
        new AdminInquiryService(
            inquiryRepository,
            new InMemoryInquiryReplyRepository(),
            wordManagementRepository,
            profanityRepository,
            new InquiryRequesterResolver(
                new InMemoryUserAccountRepository(), new InMemoryApiKeyRepository()),
            new AdminAuditService(auditLogRepository),
            eventPublisher,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("사용자 계정이 없는 과거 단어 요청도 목록과 상세를 조회한다")
  void legacyInquiryWithoutUserCanBeRead() {
    Inquiry inquiry =
        inquiryRepository.save(
            Inquiry.fromApiKey(
                InquiryType.WORD_REQUEST, "과거 단어 요청", "접수 내용", null, UUID.randomUUID(), NOW));
    var result = service.search(null, null, null, app.domain.support.PageQuery.of(0, 20));
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().getFirst().requesterName()).isNull();
    assertThat(service.findDetail(inquiry.getId()).title()).isEqualTo("과거 단어 요청");
  }

  @Nested
  @DisplayName("답변을 남기면")
  class ReplyTest {

    @Test
    @DisplayName("접수 상태였던 문의는 처리 중으로 옮긴다")
    void replyMovesReceivedToInProgress() {
      Inquiry inquiry = generalInquiry();

      var detail = service.reply(ADMIN_ID, inquiry.getId(), "확인했습니다", false);

      assertThat(detail.status()).isEqualTo(InquiryStatus.IN_PROGRESS.name());
      assertThat(detail.replies())
          .singleElement()
          .extracting(InquiryReplyView::content)
          .isEqualTo("확인했습니다");
    }

    @Test
    @DisplayName("완료로 표시하면 완료 시각을 남긴다")
    void resolveMarksResolvedAt() {
      Inquiry inquiry = generalInquiry();

      var detail = service.reply(ADMIN_ID, inquiry.getId(), "처리했습니다", true);

      assertThat(detail.status()).isEqualTo(InquiryStatus.RESOLVED.name());
      assertThat(detail.resolvedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("단어 요청 문의를 답변 완료해도 사전은 바뀌지 않는다")
    void resolvingWordInquiryDoesNotTouchDictionary() {
      Inquiry inquiry = wordInquiry("나쁜말샘플", WordRequestType.ADD);

      service.reply(ADMIN_ID, inquiry.getId(), "확인 후 회신드립니다", true);

      assertThat(profanityRepository.findAll()).isEmpty();
      assertThat(eventPublisher.eventsOf(ProfanityDictionaryChangedEvent.class)).isEmpty();
      assertThat(
              wordManagementRepository.findByInquiryId(inquiry.getId()).orElseThrow().isPending())
          .isTrue();
    }
  }

  @Nested
  @DisplayName("상태만 변경하면")
  class ChangeStatusTest {

    @Test
    @DisplayName("사전과 단어 요청 상태는 그대로 둔다")
    void statusChangeDoesNotTouchDictionary() {
      Inquiry inquiry = wordInquiry("나쁜말샘플", WordRequestType.ADD);

      var detail = service.changeStatus(ADMIN_ID, inquiry.getId(), InquiryStatus.RESOLVED);

      assertThat(detail.status()).isEqualTo(InquiryStatus.RESOLVED.name());
      assertThat(detail.wordRequest().status()).isEqualTo(WordManagementRequest.STATUS_REQUEST);
      assertThat(profanityRepository.findAll()).isEmpty();
    }
  }

  @Nested
  @DisplayName("단어 요청을 승인하면")
  class ApproveTest {

    @Test
    @DisplayName("추가 요청은 사전에 요청 출처로 등록한다")
    void approveAddRegistersWord() {
      Inquiry inquiry = wordInquiry("나쁜말샘플", WordRequestType.ADD);

      var detail = service.decideWordRequest(ADMIN_ID, inquiry.getId(), WordDecision.APPROVE, "승인");

      assertThat(detail.wordRequest().status()).isEqualTo(WordManagementRequest.STATUS_APPROVED);
      assertThat(profanityRepository.findByWord("나쁜말샘플"))
          .get()
          .satisfies(
              word -> {
                assertThat(word.getSource()).isEqualTo(WordSource.REQUEST);
                assertThat(word.getIsUsed()).isEqualTo(isUsedType.Y);
                assertThat(word.getCreatedBy()).isEqualTo(ADMIN_ID);
              });
      assertThat(eventPublisher.eventsOf(ProfanityDictionaryChangedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("제외되어 있던 단어는 다시 사용으로 되돌린다")
    void approveAddReactivatesDisabledWord() {
      profanityRepository.save(new ProfanityWord(null, "나쁜말샘플", isUsedType.N));
      Inquiry inquiry = wordInquiry("나쁜말샘플", WordRequestType.ADD);

      service.decideWordRequest(ADMIN_ID, inquiry.getId(), WordDecision.APPROVE, "승인");

      assertThat(profanityRepository.findByWord("나쁜말샘플").orElseThrow().getIsUsed())
          .isEqualTo(isUsedType.Y);
      assertThat(profanityRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("제외 요청은 사전에 있는 단어만 사용 중지로 바꾼다")
    void approveRemoveDisablesWord() {
      profanityRepository.save(ProfanityWord.create("나쁜말샘플"));
      Inquiry inquiry = wordInquiry("나쁜말샘플", WordRequestType.REMOVE);

      service.decideWordRequest(ADMIN_ID, inquiry.getId(), WordDecision.APPROVE, "승인");

      assertThat(profanityRepository.findByWord("나쁜말샘플").orElseThrow().getIsUsed())
          .isEqualTo(isUsedType.N);
    }

    @Test
    @DisplayName("사전에 없는 단어의 제외 요청은 오류로 처리한다")
    void approveRemoveOfMissingWordFails() {
      Inquiry inquiry = wordInquiry("없는단어", WordRequestType.REMOVE);

      assertThatThrownBy(
              () ->
                  service.decideWordRequest(ADMIN_ID, inquiry.getId(), WordDecision.APPROVE, "승인"))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.WORD_NOT_IN_DICTIONARY.code());
    }

    @Test
    @DisplayName("수정 요청은 변경 대상 표현이 없으므로 추정하지 않고 오류로 처리한다")
    void approveModifyIsRejectedExplicitly() {
      Inquiry inquiry = wordInquiry("나쁜말샘플", WordRequestType.MODIFY);

      assertThatThrownBy(
              () ->
                  service.decideWordRequest(ADMIN_ID, inquiry.getId(), WordDecision.APPROVE, "승인"))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.WORD_MODIFY_TARGET_REQUIRED.code());
      assertThat(profanityRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("같은 요청을 두 번 승인하지 않는다")
    void approveTwiceIsRejected() {
      Inquiry inquiry = wordInquiry("나쁜말샘플", WordRequestType.ADD);
      service.decideWordRequest(ADMIN_ID, inquiry.getId(), WordDecision.APPROVE, "승인");

      assertThatThrownBy(
              () ->
                  service.decideWordRequest(ADMIN_ID, inquiry.getId(), WordDecision.APPROVE, "재승인"))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.WORD_DECISION_ALREADY_APPLIED.code());
      assertThat(profanityRepository.findAll()).hasSize(1);
      assertThat(eventPublisher.eventsOf(ProfanityDictionaryChangedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("감사 기록을 같은 작업에서 남긴다")
    void recordsAudit() {
      Inquiry inquiry = wordInquiry("나쁜말샘플", WordRequestType.ADD);

      service.decideWordRequest(ADMIN_ID, inquiry.getId(), WordDecision.APPROVE, "정책에 부합");

      assertThat(auditLogRepository.findAll())
          .extracting(AdminAuditLog::getAction)
          .containsExactly(AdminAuditAction.WORD_REQUEST_APPROVED);
    }
  }

  @Nested
  @DisplayName("단어 요청을 거절하면")
  class RejectTest {

    @Test
    @DisplayName("사전을 변경하지 않고 상태만 거절로 남긴다")
    void rejectDoesNotTouchDictionary() {
      Inquiry inquiry = wordInquiry("나쁜말샘플", WordRequestType.ADD);

      var detail =
          service.decideWordRequest(ADMIN_ID, inquiry.getId(), WordDecision.REJECT, "정책 미해당");

      assertThat(detail.wordRequest().status()).isEqualTo(WordManagementRequest.STATUS_REJECTED);
      assertThat(profanityRepository.findAll()).isEmpty();
      assertThat(eventPublisher.eventsOf(ProfanityDictionaryChangedEvent.class)).isEmpty();
    }

    @Test
    @DisplayName("거절한 요청은 다시 승인할 수 없다")
    void rejectedRequestCannotBeApproved() {
      Inquiry inquiry = wordInquiry("나쁜말샘플", WordRequestType.ADD);
      service.decideWordRequest(ADMIN_ID, inquiry.getId(), WordDecision.REJECT, "정책 미해당");

      assertThatThrownBy(
              () ->
                  service.decideWordRequest(ADMIN_ID, inquiry.getId(), WordDecision.APPROVE, "번복"))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.WORD_DECISION_ALREADY_APPLIED.code());
    }

    @Test
    @DisplayName("단어 요청이 없는 일반 문의는 처리할 수 없다")
    void generalInquiryHasNoWordRequest() {
      Inquiry inquiry = generalInquiry();

      assertThatThrownBy(
              () ->
                  service.decideWordRequest(
                      ADMIN_ID, inquiry.getId(), WordDecision.REJECT, "해당 없음"))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.WORD_REQUEST_NOT_FOUND.code());
    }
  }

  private Inquiry generalInquiry() {
    return inquiryRepository.save(
        Inquiry.fromLoginUser(InquiryType.GENERAL, "일반 문의", "내용", REQUESTER_ID, NOW));
  }

  private Inquiry wordInquiry(String word, WordRequestType requestType) {
    Inquiry inquiry =
        inquiryRepository.save(
            Inquiry.fromLoginUser(InquiryType.WORD_REQUEST, "단어 요청", "사유", REQUESTER_ID, NOW));
    wordManagementRepository.save(
        WordManagementRequest.builder()
            .inquiryId(inquiry.getId())
            .word(word)
            .reason("사유")
            .severity("MEDIUM")
            .requestType(requestType.storedValue())
            .build());
    return inquiry;
  }
}
