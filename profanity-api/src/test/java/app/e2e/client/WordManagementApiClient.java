package app.e2e.client;

import app.core.data.response.ApiResponse;
import app.dto.request.WordRequest;
import app.presentation.WordManagementController;
import app.test.support.fixture.SeedClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * {@link WordManagementController}의 HTTP 계약을 호출하는 E2E 테스트용 client.
 *
 * <p>Controller method 이름과 public method 이름을 맞추고, 응답 판단은 테스트 본문에 둔다.
 */
public final class WordManagementApiClient {

  private static final String WORD_REQUEST_PATH = "/api/v1/word/request";
  private static final String ACCEPT_WORD_PATH = "/api/v1/word/accept/";
  private static final String API_KEY_HEADER = "x-api-key";

  private final MockMvcTester mockMvcTester;
  private final ObjectMapper objectMapper;

  public static WordManagementApiClient create(
      MockMvcTester mockMvcTester, ObjectMapper objectMapper) {
    return new WordManagementApiClient(mockMvcTester, objectMapper);
  }

  private WordManagementApiClient(MockMvcTester mockMvcTester, ObjectMapper objectMapper) {
    this.mockMvcTester = mockMvcTester;
    this.objectMapper = objectMapper;
  }

  public ApiCallResponse<ApiResponse<WordRequestResult>> requestNewWord(
      SeedClient client, WordRequest request) throws Exception {
    return requestNewWord(client.apiKey(), request);
  }

  public ApiCallResponse<ApiResponse<WordRequestResult>> requestNewWord(
      String apiKey, WordRequest request) throws Exception {
    var requestBuilder =
        mockMvcTester
            .post()
            .uri(WORD_REQUEST_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request));

    if (apiKey != null) {
      requestBuilder.header(API_KEY_HEADER, apiKey);
    }

    return ApiCallResponse.of(
        requestBuilder.exchange(),
        objectMapper,
        new TypeReference<ApiResponse<WordRequestResult>>() {});
  }

  public ApiCallResponse<ApiResponse<Boolean>> acceptWord(SeedClient client, Long requestId) {
    return acceptWord(client.apiKey(), List.of(requestId));
  }

  public ApiCallResponse<ApiResponse<Boolean>> acceptWord(
      SeedClient client, List<Long> requestIds) {
    return acceptWord(client.apiKey(), requestIds);
  }

  public ApiCallResponse<ApiResponse<Boolean>> acceptWord(String apiKey, List<Long> requestIds) {
    String requestPath =
        requestIds.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",", ACCEPT_WORD_PATH, ""));
    var requestBuilder = mockMvcTester.post().uri(requestPath);

    if (apiKey != null) {
      requestBuilder.header(API_KEY_HEADER, apiKey);
    }

    return ApiCallResponse.of(
        requestBuilder.exchange(), objectMapper, new TypeReference<ApiResponse<Boolean>>() {});
  }
}
