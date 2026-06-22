package app.e2e;

import static app.test.support.fixture.SeedClients.READ_CLIENT;
import static app.test.support.fixture.SeedClients.WRITE_CLIENT;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
  @DisplayName("READ 권한 클라이언트가 단어 요청을 승인하면 서버 에러 응답으로 말린다")
  void acceptWord_whenReadClientRequests_returnsInternalServerError() throws Exception {
    // when
    ApiCallResponse<ApiResponse<Boolean>> response = wordManagementApi.acceptWord(READ_CLIENT, 1L);

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(response.body().status().code()).isEqualTo(StatusCode.INTERNAL_SERVER_ERROR.code());
  }

  @Test
  @DisplayName("API key가 없으면 단어 등록 요청 인증 실패 응답을 반환한다")
  void requestNewWord_whenApiKeyMissing_returnsUnauthorized() throws Exception {
    // given
    WordRequest request = wordRequest("우회샘플", RequestType.ADD);

    // when
    ApiCallResponse<ApiResponse<WordRequestResult>> response =
        wordManagementApi.requestNewWord((String) null, request);

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(response.body().status().code()).isEqualTo(StatusCode.UNAUTHORIZED.code());
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

  private static WordRequest wordRequest(String word, RequestType type) {
    return WordRequest.builder()
        .word(word)
        .reason("E2E 요청 사유")
        .severity(WordSeverity.MEDIUM)
        .type(type)
        .build();
  }
}
