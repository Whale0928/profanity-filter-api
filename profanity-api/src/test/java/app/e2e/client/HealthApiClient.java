package app.e2e.client;

import app.presentation.HealthController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * {@link HealthController}의 HTTP 계약을 호출하는 E2E 테스트용 client.
 *
 * <p>Controller method 이름과 public method 이름을 맞추고, 응답 판단은 테스트 본문에 둔다.
 */
public final class HealthApiClient {

  private static final String HEALTH_PATH = "/api/v1/health";
  private static final String PING_PATH = "/api/v1/ping";

  private final MockMvcTester mockMvcTester;
  private final ObjectMapper objectMapper;

  public static HealthApiClient create(MockMvcTester mockMvcTester, ObjectMapper objectMapper) {
    return new HealthApiClient(mockMvcTester, objectMapper);
  }

  private HealthApiClient(MockMvcTester mockMvcTester, ObjectMapper objectMapper) {
    this.mockMvcTester = mockMvcTester;
    this.objectMapper = objectMapper;
  }

  public ApiCallResponse<String> health() {
    return ApiCallResponse.of(
        mockMvcTester.get().uri(HEALTH_PATH).exchange(), objectMapper, String.class);
  }

  public ApiCallResponse<String> ping() {
    return ApiCallResponse.of(
        mockMvcTester.get().uri(PING_PATH).exchange(), objectMapper, String.class);
  }
}
