package app.application.manage;

import static org.assertj.core.api.Assertions.assertThat;

import app.application.inquiry.WordRequestInquiryRegistrar;
import app.domain.InMemoryApiKeyRepository;
import app.domain.InMemoryInquiryRepository;
import app.domain.InMemoryWordManagementRepository;
import app.domain.inquiry.Inquiry;
import app.domain.inquiry.InquiryStatus;
import app.domain.inquiry.InquiryType;
import app.domain.manage.WordManagementRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("기존 외부 단어 요청 API")
class DefaultWordManagementServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-10T00:00:00Z");
  private static final UUID API_KEY_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

  private InMemoryInquiryRepository inquiryRepository;
  private InMemoryWordManagementRepository wordManagementRepository;
  private DefaultWordManagementService service;

  @BeforeEach
  void setUp() {
    inquiryRepository = new InMemoryInquiryRepository();
    wordManagementRepository = new InMemoryWordManagementRepository();
    service =
        new DefaultWordManagementService(
            new WordRequestInquiryRegistrar(
                inquiryRepository,
                wordManagementRepository,
                new InMemoryApiKeyRepository(),
                Clock.fixed(NOW, ZoneOffset.UTC)));
  }

  @Nested
  @DisplayName("단어 요청을 받으면")
  class RegisterTest {

    @Test
    @DisplayName("요청과 문의를 함께 만든다")
    void createsInquiryWithWordRequest() {
      service.requestNewWord(API_KEY_ID, "나쁜말샘플", "필터링이 필요합니다", "MEDIUM");

      var inquiries = inquiryRepository.searchForAdmin(null, null, null, 0, 20);
      assertThat(inquiries.items())
          .singleElement()
          .satisfies(
              inquiry -> {
                assertThat(inquiry.getType()).isEqualTo(InquiryType.WORD_REQUEST);
                assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.RECEIVED);
                assertThat(inquiry.getTitle()).isEqualTo("단어 요청: 나쁜말샘플");
                assertThat(inquiry.getContent()).isEqualTo("필터링이 필요합니다");
              });
    }

    @Test
    @DisplayName("요청자 식별자는 기존과 같이 API Key 식별자로 남긴다")
    void keepsApiKeyIdAsRequestUserId() {
      service.requestNewWord(API_KEY_ID, "나쁜말샘플", "사유", "MEDIUM");

      assertThat(wordManagementRepository.findAll())
          .singleElement()
          .satisfies(
              request -> {
                assertThat(request.getRequestUserId()).isEqualTo(API_KEY_ID);
                assertThat(request.getInquiryId()).isNotNull();
              });
    }

    @Test
    @DisplayName("소유자를 확인할 수 없는 API Key는 문의에 사용자를 연결하지 않는다")
    void unknownApiKeyLeavesRequesterUserIdEmpty() {
      service.requestNewWord(API_KEY_ID, "나쁜말샘플", "사유", "MEDIUM");

      Inquiry inquiry = inquiryRepository.searchForAdmin(null, null, null, 0, 20).items().get(0);
      assertThat(inquiry.getRequesterUserId()).isNull();
      assertThat(inquiry.getRequesterApiKeyId()).isEqualTo(API_KEY_ID);
    }

    @Test
    @DisplayName("추가, 제외, 수정 요청의 저장 값을 기존과 동일하게 유지한다")
    void keepsLegacyStoredRequestTypes() {
      service.requestNewWord(API_KEY_ID, "단어1", "사유", "LOW");
      service.exceptionWord(API_KEY_ID, "단어2", "사유", "LOW");
      service.modifyWord(API_KEY_ID, "단어3", "사유", "LOW");

      assertThat(wordManagementRepository.findAll())
          .extracting(WordManagementRequest::getRequestType)
          .containsExactly("NEW", "EXCEPTION", "MODIFY");
    }

    @Test
    @DisplayName("새 요청은 대기 상태로 시작한다")
    void startsAsPending() {
      service.requestNewWord(API_KEY_ID, "나쁜말샘플", "사유", "MEDIUM");

      assertThat(wordManagementRepository.findAll())
          .singleElement()
          .matches(WordManagementRequest::isPending);
    }
  }
}
