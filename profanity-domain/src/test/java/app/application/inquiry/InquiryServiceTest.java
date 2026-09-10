package app.application.inquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.InMemoryApiKeyRepository;
import app.domain.InMemoryInquiryReplyRepository;
import app.domain.InMemoryInquiryRepository;
import app.domain.InMemoryUserAccountRepository;
import app.domain.InMemoryWordManagementRepository;
import app.domain.manage.WordManagementRequest;
import app.domain.support.PageQuery;
import app.domain.user.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("로그인 사용자 문의")
class InquiryServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-10T00:00:00Z");
  private static final UUID OWNER_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
  private static final UUID OTHER_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");

  private InMemoryInquiryRepository inquiryRepository;
  private InMemoryWordManagementRepository wordManagementRepository;
  private InMemoryUserAccountRepository userAccountRepository;
  private InquiryService service;

  @BeforeEach
  void setUp() {
    inquiryRepository = new InMemoryInquiryRepository();
    wordManagementRepository = new InMemoryWordManagementRepository();
    userAccountRepository = new InMemoryUserAccountRepository();
    InMemoryApiKeyRepository apiKeyRepository = new InMemoryApiKeyRepository();
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    saveUser(OWNER_ID, "소유자", "owner@example.test");
    saveUser(OTHER_ID, "다른 사용자", "other@example.test");

    service =
        new InquiryService(
            inquiryRepository,
            new InMemoryInquiryReplyRepository(),
            new InquiryRequesterResolver(userAccountRepository, apiKeyRepository),
            new WordRequestInquiryRegistrar(
                inquiryRepository, wordManagementRepository, apiKeyRepository, clock),
            wordManagementRepository,
            clock);
  }

  @Nested
  @DisplayName("문의를 등록하면")
  class CreateTest {

    @Test
    @DisplayName("일반 문의는 단어 요청을 만들지 않는다")
    void generalInquiryHasNoWordRequest() {
      var created = service.create(OWNER_ID, general("결제 문의"));

      assertThat(created.wordRequest()).isNull();
      assertThat(wordManagementRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("단어 요청은 같은 작업에서 단어 요청까지 함께 만든다")
    void wordRequestInquiryCreatesWordRequest() {
      var created = service.create(OWNER_ID, wordRequest("나쁜말샘플", "ADD"));

      assertThat(created.wordRequest()).isNotNull();
      assertThat(created.wordRequest().word()).isEqualTo("나쁜말샘플");
      assertThat(created.wordRequest().requestType()).isEqualTo("ADD");
      assertThat(created.wordRequest().status()).isEqualTo(WordManagementRequest.STATUS_REQUEST);
    }

    @Test
    @DisplayName("대시보드 단어 요청은 API Key 식별자를 요구하지 않는다")
    void dashboardWordRequestHasNoApiKeyId() {
      service.create(OWNER_ID, wordRequest("나쁜말샘플", "ADD"));

      assertThat(wordManagementRepository.findAll())
          .singleElement()
          .satisfies(
              request -> {
                assertThat(request.getRequestUserId()).isNull();
                assertThat(request.getInquiryId()).isNotNull();
              });
    }

    @Test
    @DisplayName("기존 외부 API의 요청 타입 의미를 그대로 저장한다")
    void keepsLegacyStoredRequestType() {
      service.create(OWNER_ID, wordRequest("나쁜말샘플", "REMOVE"));

      assertThat(wordManagementRepository.findAll())
          .singleElement()
          .extracting(WordManagementRequest::getRequestType)
          .isEqualTo("EXCEPTION");
    }

    @Test
    @DisplayName("단어 요청에 단어가 없으면 거절한다")
    void wordRequestRequiresWord() {
      assertThatThrownBy(
              () -> NewInquiryCommand.of("WORD_REQUEST", "제목", "내용", null, "ADD", "MEDIUM"))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Nested
  @DisplayName("문의를 조회하면")
  class ReadTest {

    @Test
    @DisplayName("본인이 등록한 문의만 목록에 나온다")
    void listContainsOnlyOwnInquiries() {
      service.create(OWNER_ID, general("내 문의"));
      service.create(OTHER_ID, general("남의 문의"));

      var result = service.findMine(OWNER_ID, null, PageQuery.of(null, null));

      assertThat(result.items()).extracting(InquiryView::title).containsExactly("내 문의");
    }

    @Test
    @DisplayName("본인 문의 상세에는 요청자 정보가 담긴다")
    void detailContainsRequester() {
      var created = service.create(OWNER_ID, general("내 문의"));

      var found = service.findMineDetail(OWNER_ID, created.id());

      assertThat(found.requesterName()).isEqualTo("소유자");
      assertThat(found.requesterEmail()).isEqualTo("owner@example.test");
    }

    @Test
    @DisplayName("다른 사용자의 문의는 접근을 거절한다")
    void otherUserInquiryIsDenied() {
      var created = service.create(OTHER_ID, general("남의 문의"));

      assertThatThrownBy(() -> service.findMineDetail(OWNER_ID, created.id()))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.INQUIRY_ACCESS_DENIED.code());
    }

    @Test
    @DisplayName("없는 문의는 찾을 수 없다고 응답한다")
    void missingInquiryIsNotFound() {
      assertThatThrownBy(() -> service.findMineDetail(OWNER_ID, 404L))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.INQUIRY_NOT_FOUND.code());
    }
  }

  private static NewInquiryCommand general(String title) {
    return NewInquiryCommand.of("GENERAL", title, "내용", null, null, null);
  }

  private static NewInquiryCommand wordRequest(String word, String requestType) {
    return NewInquiryCommand.of("WORD_REQUEST", "단어 요청", "사유", word, requestType, "MEDIUM");
  }

  /** 테스트에서 사용자 식별자를 고정하기 위해 생성 직후 id를 지정합니다. */
  private void saveUser(UUID id, String displayName, String email) {
    UserAccount user = UserAccount.create(displayName, email, null, NOW);
    try {
      var field = UserAccount.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(user, id);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
    userAccountRepository.save(user);
  }
}
