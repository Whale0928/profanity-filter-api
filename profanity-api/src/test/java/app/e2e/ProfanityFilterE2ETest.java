package app.e2e;

import static app.test.support.fixture.SeedClients.READ_CLIENT;
import static app.test.support.fixture.SeedWords.ACTIVE_PROFANITY_SAMPLE;
import static app.test.support.fixture.SeedWords.ACTIVE_SLANG_SAMPLE;
import static org.assertj.core.api.Assertions.assertThat;

import app.core.data.constant.Mode;
import app.core.data.response.ApiResponse;
import app.core.data.response.FilterApiResponse;
import app.core.data.response.constant.StatusCode;
import app.dto.request.ApiRequest;
import app.e2e.client.ApiCallResponse;
import app.e2e.client.ProfanityApiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ProfanityFilterE2ETest extends AbstractApiTester {

  private ProfanityApiClient profanityApi;

  @BeforeEach
  void setUpProfanityFilterE2ETest() {
    profanityApi = ProfanityApiClient.create(mockMvcTester, objectMapper);
  }

  @Test
  @DisplayName("인증된 클라이언트가 DB 단어 기준으로 필터링하고 요청 이력을 저장한다")
  void filter_whenAuthenticatedClientRequestsWithSeededWord_masksTextAndRecordsTracking()
      throws Exception {
    // given
    String requestText = "문장 안에 " + ACTIVE_PROFANITY_SAMPLE.word() + " 이 포함된다";

    // when
    ApiCallResponse<FilterApiResponse> apiResponse =
        profanityApi.basicProfanity(READ_CLIENT, requestText, Mode.FILTER);
    FilterApiResponse response = apiResponse.body();

    // then
    assertThat(apiResponse.result()).hasStatusOk();
    assertThat(response.trackingId()).isNotNull();
    assertThat(response.status().code()).isEqualTo(2000);
    assertThat(response.detected())
        .anyMatch(detected -> detected.filteredWord().equals(ACTIVE_PROFANITY_SAMPLE.word()));
    assertThat(response.filtered()).isEqualTo("문장 안에 ***** 이 포함된다");
    recordProbe.assertFilterRecord(READ_CLIENT, requestText, ACTIVE_PROFANITY_SAMPLE);
  }

  @Test
  @DisplayName("FILTER 모드에서 감지 단어가 없으면 원문을 유지하고 빈 감지 결과를 저장한다")
  void filter_whenNoSeededWordExists_keepsOriginalTextAndRecordsEmptyWords() throws Exception {
    // given
    String requestText = "평범한 문장만 포함된다";

    // when
    ApiCallResponse<FilterApiResponse> apiResponse =
        profanityApi.basicProfanity(READ_CLIENT, requestText, Mode.FILTER);
    FilterApiResponse response = apiResponse.body();

    // then
    assertThat(apiResponse.result()).hasStatusOk();
    assertThat(response.status().code()).isEqualTo(StatusCode.OK.code());
    assertThat(response.detected()).isEmpty();
    assertThat(response.filtered()).isEqualTo(requestText);
    assertThat(recordProbe.countRecords(READ_CLIENT, requestText, Mode.FILTER, "")).isEqualTo(1);
  }

  @Test
  @DisplayName("NORMAL 모드는 마스킹 없이 감지된 모든 단어를 반환하고 요청 이력을 저장한다")
  void filter_whenNormalModeRequestsMultipleSeededWords_returnsAllDetectedWordsAndRecords()
      throws Exception {
    // given
    String requestText =
        ACTIVE_PROFANITY_SAMPLE.word() + " 과 " + ACTIVE_SLANG_SAMPLE.word() + " 이 함께 있다";

    // when
    ApiCallResponse<FilterApiResponse> apiResponse =
        profanityApi.basicProfanity(READ_CLIENT, requestText, Mode.NORMAL);
    FilterApiResponse response = apiResponse.body();

    // then
    assertThat(apiResponse.result()).hasStatusOk();
    assertThat(response.status().code()).isEqualTo(StatusCode.OK.code());
    assertThat(response.detected())
        .extracting("filteredWord")
        .containsExactlyInAnyOrder(ACTIVE_PROFANITY_SAMPLE.word(), ACTIVE_SLANG_SAMPLE.word());
    assertThat(response.filtered()).isEmpty();
    assertThat(recordProbe.countRecords(READ_CLIENT, requestText, Mode.NORMAL)).isEqualTo(1);
  }

  @Test
  @DisplayName("QUICK 모드는 첫 번째 감지 단어만 반환하고 요청 이력을 저장한다")
  void filter_whenQuickModeRequestsMultipleSeededWords_returnsFirstDetectedWordAndRecords()
      throws Exception {
    // given
    String requestText =
        ACTIVE_SLANG_SAMPLE.word() + " 다음에 " + ACTIVE_PROFANITY_SAMPLE.word() + " 이 있다";

    // when
    ApiCallResponse<FilterApiResponse> apiResponse =
        profanityApi.basicProfanity(READ_CLIENT, requestText, Mode.QUICK);
    FilterApiResponse response = apiResponse.body();

    // then
    assertThat(apiResponse.result()).hasStatusOk();
    assertThat(response.status().code()).isEqualTo(StatusCode.OK.code());
    assertThat(response.detected()).hasSize(1);
    assertThat(response.detected())
        .anyMatch(detected -> detected.filteredWord().equals(ACTIVE_SLANG_SAMPLE.word()));
    assertThat(response.filtered()).isEmpty();
    assertThat(recordProbe.countRecords(READ_CLIENT, requestText, Mode.QUICK)).isEqualTo(1);
  }

  @Test
  @DisplayName("JSON 요청의 mode는 대소문자를 구분하지 않고 처리한다")
  void filter_whenModeUsesLowercaseJsonValue_processesCaseInsensitiveMode() throws Exception {
    // given
    String requestText = "문장 안에 " + ACTIVE_PROFANITY_SAMPLE.word() + " 이 포함된다";
    String requestBody = "{\"text\":\"" + requestText + "\",\"mode\":\"filter\"}";

    // when
    var response =
        mockMvcTester
            .post()
            .uri("/api/v1/filter")
            .header("x-api-key", READ_CLIENT.apiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .exchange();
    FilterApiResponse body =
        objectMapper.readValue(
            response.getResponse().getContentAsString(), FilterApiResponse.class);

    // then
    assertThat(response).hasStatusOk();
    assertThat(body.status().code()).isEqualTo(StatusCode.OK.code());
    assertThat(body.filtered()).isEqualTo("문장 안에 ***** 이 포함된다");
  }

  @Test
  @DisplayName("form-urlencoded 요청도 필터링하고 요청 이력을 저장한다")
  void basicProfanityByUrlencodedValue_whenAuthenticatedClientRequests_savesRecord()
      throws Exception {
    // given
    String requestText = "form " + ACTIVE_PROFANITY_SAMPLE.word();

    // when
    ApiCallResponse<FilterApiResponse> apiResponse =
        profanityApi.basicProfanityByUrlencodedValue(READ_CLIENT, requestText, Mode.FILTER);
    FilterApiResponse response = apiResponse.body();

    // then
    assertThat(apiResponse.result()).hasStatusOk();
    assertThat(response.status().code()).isEqualTo(StatusCode.OK.code());
    assertThat(response.filtered()).isEqualTo("form *****");
    assertThat(recordProbe.countRecords(READ_CLIENT, requestText, Mode.FILTER)).isEqualTo(1);
  }

  @Test
  @DisplayName("advanced 엔드포인트는 word 파라미터를 FILTER 모드처럼 마스킹한다")
  void advancedProfanity_whenWordContainsSeededWord_returnsMaskedText() throws Exception {
    // given
    String word = "advanced " + ACTIVE_PROFANITY_SAMPLE.word();

    // when
    ApiCallResponse<FilterApiResponse> apiResponse =
        profanityApi.advancedProfanity(READ_CLIENT, word);
    FilterApiResponse response = apiResponse.body();

    // then
    assertThat(apiResponse.result()).hasStatusOk();
    assertThat(response.status().code()).isEqualTo(StatusCode.OK.code());
    assertThat(response.filtered()).isEqualTo("advanced *****");
  }

  @Test
  @DisplayName("API key가 없으면 필터링 요청 인증 실패 응답을 반환한다")
  void filter_whenApiKeyMissing_returnsUnauthorized() throws Exception {
    // given
    ApiRequest request = ApiRequest.builder().text("평범한 문장").mode(Mode.FILTER).build();

    // when
    var response =
        mockMvcTester
            .post()
            .uri("/api/v1/filter")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .exchange();
    ApiResponse<Void> body =
        objectMapper.readValue(
            response.getResponse().getContentAsString(), new TypeReference<>() {});

    // then
    assertThat(response).hasStatusOk();
    assertThat(body.status().code()).isEqualTo(StatusCode.UNAUTHORIZED.code());
  }

  @Test
  @DisplayName("필터링 대상 문자열이 비어 있으면 검증 실패 응답을 반환한다")
  void filter_whenTextBlank_returnsBadRequest() throws Exception {
    // given
    ApiRequest request = ApiRequest.builder().text("").mode(Mode.FILTER).build();

    // when
    ApiCallResponse<FilterApiResponse> apiResponse =
        profanityApi.basicProfanity(READ_CLIENT, request);

    // then
    assertThat(apiResponse.result()).hasStatusOk();
    assertThat(apiResponse.errorBody().status().code()).isEqualTo(StatusCode.BAD_REQUEST.code());
  }

  @Test
  @DisplayName("알 수 없는 mode 문자열이면 검증 실패 응답을 반환한다")
  void filter_whenModeUnknown_returnsBadRequest() throws Exception {
    // given
    String requestBody = "{\"text\":\"평범한 문장\",\"mode\":\"UNKNOWN\"}";

    // when
    var response =
        mockMvcTester
            .post()
            .uri("/api/v1/filter")
            .header("x-api-key", READ_CLIENT.apiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .exchange();
    ApiResponse<Void> body =
        objectMapper.readValue(
            response.getResponse().getContentAsString(), new TypeReference<>() {});

    // then
    assertThat(response).hasStatusOk();
    assertThat(body.status().code()).isEqualTo(StatusCode.BAD_REQUEST.code());
  }
}
