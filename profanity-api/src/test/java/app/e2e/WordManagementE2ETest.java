package app.e2e;

import static app.test.support.fixture.SeedApiKeys.READ_CLIENT;
import static app.test.support.fixture.SeedApiKeys.WRITE_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

import app.core.data.response.ApiResponse;
import app.core.data.response.constant.StatusCode;
import app.dto.request.WordRequest;
import app.dto.request.WordRequest.RequestType;
import app.dto.request.WordRequest.WordSeverity;
import app.e2e.client.ApiCallResponse;
import app.e2e.client.WordManagementApiClient;
import app.e2e.client.WordRequestResult;
import app.test.support.reader.WordManagementRequestReader;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class WordManagementE2ETest extends AbstractApiTester {

  @Autowired private javax.sql.DataSource dataSource;

  private WordManagementApiClient wordManagementApi;
  private WordManagementRequestReader wordManagementRequests;

  @BeforeEach
  void setUpWordManagementE2ETest() {
    wordManagementApi = WordManagementApiClient.create(mockMvcTester, objectMapper);
    wordManagementRequests = new WordManagementRequestReader(dataSource);
  }

  @Test
  @DisplayName("인증된 클라이언트가 신규 단어 등록을 요청하면 요청 이력을 저장한다")
  void requestNewWord_whenAuthenticatedClientRequestsAddWord_savesRequest() throws Exception {
    // given
    String word = "우회샘플";
    WordRequest request = wordRequest(word, RequestType.ADD);

    // when
    ApiCallResponse<ApiResponse<WordRequestResult>> response =
        wordManagementApi.requestNewWord(READ_CLIENT, request);
    ApiResponse<WordRequestResult> body = response.body();

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(body.status().code()).isEqualTo(StatusCode.OK.code());
    assertThat(body.data().result()).isTrue();
    assertThat(wordManagementRequests.countRequests(READ_CLIENT, word, "NEW")).isEqualTo(1);
  }

  @Test
  @DisplayName("인증된 클라이언트가 단어 제외를 요청하면 EXCEPTION 요청 이력을 저장한다")
  void requestNewWord_whenAuthenticatedClientRequestsRemoveWord_savesExceptionRequest()
      throws Exception {
    // given
    String word = "제외요청샘플";
    WordRequest request = wordRequest(word, RequestType.REMOVE);

    // when
    ApiCallResponse<ApiResponse<WordRequestResult>> response =
        wordManagementApi.requestNewWord(READ_CLIENT, request);
    ApiResponse<WordRequestResult> body = response.body();

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(body.status().code()).isEqualTo(StatusCode.OK.code());
    assertThat(body.data().result()).isTrue();
    assertThat(wordManagementRequests.countRequests(READ_CLIENT, word, "EXCEPTION")).isEqualTo(1);
  }

  @Test
  @DisplayName("인증된 클라이언트가 단어 수정을 요청하면 MODIFY 요청 이력을 저장한다")
  void requestNewWord_whenAuthenticatedClientRequestsModifyWord_savesModifyRequest()
      throws Exception {
    // given
    String word = "수정요청샘플";
    WordRequest request = wordRequest(word, RequestType.MODIFY);

    // when
    ApiCallResponse<ApiResponse<WordRequestResult>> response =
        wordManagementApi.requestNewWord(READ_CLIENT, request);
    ApiResponse<WordRequestResult> body = response.body();

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(body.status().code()).isEqualTo(StatusCode.OK.code());
    assertThat(body.data().result()).isTrue();
    assertThat(wordManagementRequests.countRequests(READ_CLIENT, word, "MODIFY")).isEqualTo(1);
  }

  @Test
  @DisplayName("요청 사유가 500자이면 단어 등록 요청을 저장한다")
  void requestNewWord_whenReasonLengthIsMax_savesRequest() throws Exception {
    // given
    String word = "경계값샘플";
    WordRequest request =
        WordRequest.builder()
            .word(word)
            .reason("가".repeat(500))
            .severity(WordSeverity.MEDIUM)
            .type(RequestType.ADD)
            .build();

    // when
    ApiCallResponse<ApiResponse<WordRequestResult>> response =
        wordManagementApi.requestNewWord(READ_CLIENT, request);

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(response.body().status().code()).isEqualTo(StatusCode.OK.code());
    assertThat(wordManagementRequests.countRequests(READ_CLIENT, word, "NEW")).isEqualTo(1);
  }

  @Test
  @DisplayName("WRITE 권한 클라이언트가 단어 요청을 승인하면 false 응답을 반환한다")
  void acceptWord_whenWriteClientRequests_returnsCurrentServiceResult() throws Exception {
    // when
    ApiCallResponse<ApiResponse<Boolean>> response = wordManagementApi.acceptWord(WRITE_CLIENT, 1L);
    ApiResponse<Boolean> body = response.body();

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(body.status().code()).isEqualTo(StatusCode.OK.code());
    assertThat(body.data()).isFalse();
  }

  @Test
  @DisplayName("WRITE 권한 클라이언트가 여러 단어 요청을 승인하면 현재 서비스 결과를 반환한다")
  void acceptWord_whenWriteClientRequestsMultipleIds_returnsCurrentServiceResult()
      throws Exception {
    // when
    ApiCallResponse<ApiResponse<Boolean>> response =
        wordManagementApi.acceptWord(WRITE_CLIENT, java.util.List.of(1L, 2L));
    ApiResponse<Boolean> body = response.body();

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(body.status().code()).isEqualTo(StatusCode.OK.code());
    assertThat(body.data()).isFalse();
  }

  @Test
  @DisplayName("READ 권한 클라이언트가 단어 요청을 승인하면 권한 오류를 반환한다")
  void acceptWord_whenReadClientRequests_returnsBadRequest() throws Exception {
    // when
    ApiCallResponse<ApiResponse<Boolean>> response = wordManagementApi.acceptWord(READ_CLIENT, 1L);

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(response.body().status().code()).isEqualTo(StatusCode.BAD_REQUEST.code());
  }

  @Test
  @DisplayName("단어 요청 승인 id가 숫자가 아니면 잘못된 요청 응답을 반환한다")
  void acceptWord_whenRequestIdIsNotNumber_returnsBadRequest() throws Exception {
    // when
    var response =
        mockMvcTester
            .post()
            .uri("/api/v1/word/accept/not-number")
            .header("x-api-key", WRITE_CLIENT.apiKey())
            .exchange();
    ApiResponse<Void> body =
        objectMapper.readValue(
            response.getResponse().getContentAsString(), new TypeReference<>() {});

    // then
    assertThat(response).hasStatusOk();
    assertThat(body.status().code()).isEqualTo(StatusCode.BAD_REQUEST.code());
  }

  @Test
  @DisplayName("API key가 없으면 단어 등록 요청 인증 실패 응답을 반환한다")
  void requestNewWord_whenApiKeyMissing_returnsUnauthorized() throws Exception {
    // given
    WordRequest request = wordRequest("우회샘플", RequestType.ADD);

    // when
    var response =
        mockMvcTester
            .post()
            .uri("/api/v1/word/request")
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
  @DisplayName("필수 요청값이 없으면 단어 등록 요청 검증 실패 응답을 반환한다")
  void requestNewWord_whenRequiredValueMissing_returnsBadRequest() throws Exception {
    // given
    WordRequest request =
        WordRequest.builder()
            .word("")
            .reason("")
            .severity(WordSeverity.MEDIUM)
            .type(RequestType.ADD)
            .build();

    // when
    ApiCallResponse<ApiResponse<WordRequestResult>> response =
        wordManagementApi.requestNewWord(READ_CLIENT, request);

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(response.body().status().code()).isEqualTo(StatusCode.BAD_REQUEST.code());
  }

  @Test
  @DisplayName("요청 사유가 500자를 초과하면 단어 등록 요청 검증 실패 응답을 반환한다")
  void requestNewWord_whenReasonLengthExceedsMax_returnsBadRequest() throws Exception {
    // given
    WordRequest request =
        WordRequest.builder()
            .word("경계초과샘플")
            .reason("가".repeat(501))
            .severity(WordSeverity.MEDIUM)
            .type(RequestType.ADD)
            .build();

    // when
    ApiCallResponse<ApiResponse<WordRequestResult>> response =
        wordManagementApi.requestNewWord(READ_CLIENT, request);

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(response.body().status().code()).isEqualTo(StatusCode.BAD_REQUEST.code());
  }

  @Test
  @DisplayName("알 수 없는 요청 타입이면 단어 등록 요청 검증 실패 응답을 반환한다")
  void requestNewWord_whenRequestTypeUnknown_returnsBadRequest() throws Exception {
    // given
    String requestBody =
        """
        {
          "word": "타입오류샘플",
          "reason": "E2E 요청 사유",
          "severity": "MEDIUM",
          "type": "UNKNOWN"
        }
        """;

    // when
    var response =
        mockMvcTester
            .post()
            .uri("/api/v1/word/request")
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

  @Test
  @DisplayName("심각도가 없으면 단어 등록 요청 검증 실패 응답을 반환한다")
  void requestNewWord_whenSeverityMissing_returnsBadRequest() throws Exception {
    // given
    WordRequest request =
        WordRequest.builder().word("심각도누락샘플").reason("E2E 요청 사유").type(RequestType.ADD).build();

    // when
    ApiCallResponse<ApiResponse<WordRequestResult>> response =
        wordManagementApi.requestNewWord(READ_CLIENT, request);

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(response.body().status().code()).isEqualTo(StatusCode.BAD_REQUEST.code());
  }

  private static WordRequest wordRequest(String word, RequestType type) {
    return WordRequest.builder()
        .word(word)
        .reason("E2E 요청 사유")
        .severity(WordSeverity.MEDIUM)
        .type(type)
        .build();
  }
}
