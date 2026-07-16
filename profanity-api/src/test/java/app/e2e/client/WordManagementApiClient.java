package app.e2e.client;

import app.core.data.response.ApiResponse;
import app.dto.request.WordRequest;
import app.presentation.WordManagementController;
import app.test.support.fixture.SeedApiKey;
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
      SeedApiKey client, WordRequest request) throws Exception {
    var result =
        mockMvcTester
            .post()
            .uri(WORD_REQUEST_PATH)
            .header(API_KEY_HEADER, client.apiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .exchange();

    return ApiCallResponse.of(
        result, objectMapper, new TypeReference<ApiResponse<WordRequestResult>>() {});
  }

  public ApiCallResponse<ApiResponse<Boolean>> acceptWord(SeedApiKey client, Long requestId) {
    return acceptWord(client, List.of(requestId));
  }

  public ApiCallResponse<ApiResponse<Boolean>> acceptWord(
      SeedApiKey client, List<Long> requestIds) {
    String requestPath =
        requestIds.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",", ACCEPT_WORD_PATH, ""));
    var result =
        mockMvcTester.post().uri(requestPath).header(API_KEY_HEADER, client.apiKey()).exchange();

    return ApiCallResponse.of(result, objectMapper, new TypeReference<ApiResponse<Boolean>>() {});
  }
}
