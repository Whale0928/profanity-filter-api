package app.e2e.client;

import app.core.data.manage.response.ResultMessage;
import app.presentation.SyncController;
import app.test.support.fixture.SeedClient;
import app.test.support.fixture.SeedManageAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * {@link SyncController}의 HTTP 계약을 호출하는 E2E 테스트용 client.
 *
 * <p>Controller method 이름과 public method 이름을 맞추고, 응답 판단은 테스트 본문에 둔다.
 */
public final class SyncApiClient {

  private static final String SYNC_PATH = "/api/v1/sync";
  private static final String API_KEY_HEADER = "x-api-key";

  private final MockMvcTester mockMvcTester;
  private final ObjectMapper objectMapper;

  public static SyncApiClient create(MockMvcTester mockMvcTester, ObjectMapper objectMapper) {
    return new SyncApiClient(mockMvcTester, objectMapper);
  }

  private SyncApiClient(MockMvcTester mockMvcTester, ObjectMapper objectMapper) {
    this.mockMvcTester = mockMvcTester;
    this.objectMapper = objectMapper;
  }

  public ApiCallResponse<ResultMessage> doSync(SeedClient client, SeedManageAccount account) {
    return doSync(client, account.password());
  }

  public ApiCallResponse<ResultMessage> doSync(SeedClient client, String password) {
    return doSync(client.apiKey(), password);
  }

  public ApiCallResponse<ResultMessage> doSync(String apiKey, String password) {
    if (apiKey == null && password == null) {
      return ApiCallResponse.of(
          mockMvcTester.get().uri(SYNC_PATH).exchange(), objectMapper, ResultMessage.class);
    }

    if (apiKey == null) {
      return ApiCallResponse.of(
          mockMvcTester.get().uri(SYNC_PATH + "?password={password}", password).exchange(),
          objectMapper,
          ResultMessage.class);
    }

    if (password == null) {
      return ApiCallResponse.of(
          mockMvcTester.get().uri(SYNC_PATH).header(API_KEY_HEADER, apiKey).exchange(),
          objectMapper,
          ResultMessage.class);
    }

    return ApiCallResponse.of(
        mockMvcTester
            .get()
            .uri(SYNC_PATH + "?password={password}", password)
            .header(API_KEY_HEADER, apiKey)
            .exchange(),
        objectMapper,
        ResultMessage.class);
  }
}
