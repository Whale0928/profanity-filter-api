package app.e2e;

import static app.test.support.fixture.SeedApiKeys.READ_CLIENT;
import static app.test.support.fixture.SeedManageAccounts.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;

import app.application.client.APIKeyGenerator;
import app.core.data.manage.response.ResultMessage;
import app.core.data.response.ApiResponse;
import app.core.data.response.constant.StatusCode;
import app.e2e.client.ApiCallResponse;
import app.e2e.client.SyncApiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class SyncE2ETest extends AbstractApiTester {

  @Autowired private APIKeyGenerator apiKeyGenerator;

  private SyncApiClient syncApi;

  @BeforeEach
  void setUpSyncE2ETest() {
    syncApi = SyncApiClient.create(mockMvcTester, objectMapper);
  }

  @Test
  @DisplayName("관리자 계정으로 단어 동기화를 요청하면 성공 응답을 반환한다")
  void doSync_whenSeededAdminRequests_returnsSuccessMessage() throws Exception {
    // when
    ApiCallResponse<ResultMessage> response = syncApi.doSync(READ_CLIENT, ADMIN);

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(response.body()).isEqualTo(ResultMessage.SUCCESS_SYNC_WORD);
  }

  @Test
  @DisplayName("API key가 없으면 인증 실패 응답을 반환한다")
  void doSync_whenApiKeyMissing_returnsUnauthorized() throws Exception {
    // when
    MvcTestResult response = doSyncHttp(null, ADMIN.password());
    ApiResponse<Void> body = readErrorBody(response);

    // then
    assertThat(response).hasStatusOk();
    assertThat(body.status().code()).isEqualTo(StatusCode.UNAUTHORIZED.code());
  }

  @Test
  @DisplayName("API key 형식이 올바르지 않으면 인증 실패 응답을 반환한다")
  void doSync_whenApiKeyFormatInvalid_returnsInvalidApiKey() throws Exception {
    // when
    MvcTestResult response = doSyncHttp("invalid-api-key", ADMIN.password());
    ApiResponse<Void> body = readErrorBody(response);

    // then
    assertThat(response).hasStatusOk();
    assertThat(body.status().code()).isEqualTo(StatusCode.INVALID_API_KEY.code());
  }

  @Test
  @DisplayName("존재하지 않는 API key면 클라이언트 조회 실패 응답을 반환한다")
  void doSync_whenApiKeyDoesNotExist_returnsNotFoundClient() throws Exception {
    // given
    String unknownApiKey = apiKeyGenerator.generateApiKey();

    // when
    MvcTestResult response = doSyncHttp(unknownApiKey, ADMIN.password());
    ApiResponse<Void> body = readErrorBody(response);

    // then
    assertThat(response).hasStatusOk();
    assertThat(body.status().code()).isEqualTo(StatusCode.NOT_FOUND_CLIENT.code());
  }

  @Test
  @DisplayName("password 파라미터가 없으면 잘못된 요청 응답을 반환한다")
  void doSync_whenPasswordParameterMissing_returnsBadRequest() throws Exception {
    // when
    MvcTestResult response = doSyncHttp(READ_CLIENT.apiKey(), null);
    ApiResponse<Void> body = readErrorBody(response);

    // then
    assertThat(response).hasStatusOk();
    assertThat(body.status().code()).isEqualTo(StatusCode.BAD_REQUEST.code());
  }

  @Test
  @DisplayName("등록되지 않은 관리자 password면 서버 에러 응답으로 말린다")
  void doSync_whenAdminPasswordUnknown_returnsInternalServerError() throws Exception {
    // when
    ApiCallResponse<ResultMessage> response = syncApi.doSync(READ_CLIENT, "unknown-admin-password");

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(response.errorBody().status().code())
        .isEqualTo(StatusCode.INTERNAL_SERVER_ERROR.code());
  }

  @Test
  @DisplayName("관리자 password가 빈 값이면 서버 에러 응답으로 말린다")
  void doSync_whenAdminPasswordBlank_returnsInternalServerError() throws Exception {
    // when
    ApiCallResponse<ResultMessage> response = syncApi.doSync(READ_CLIENT, "");

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(response.errorBody().status().code())
        .isEqualTo(StatusCode.INTERNAL_SERVER_ERROR.code());
  }

  private MvcTestResult doSyncHttp(String apiKey, String password) {
    if (password == null) {
      var request = mockMvcTester.get().uri("/api/v1/sync");
      if (apiKey != null) {
        request.header("x-api-key", apiKey);
      }
      return request.exchange();
    }

    var request = mockMvcTester.get().uri("/api/v1/sync?password={password}", password);
    if (apiKey != null) {
      request.header("x-api-key", apiKey);
    }
    return request.exchange();
  }

  private ApiResponse<Void> readErrorBody(MvcTestResult response) throws Exception {
    return objectMapper.readValue(
        response.getResponse().getContentAsString(), new TypeReference<>() {});
  }
}
