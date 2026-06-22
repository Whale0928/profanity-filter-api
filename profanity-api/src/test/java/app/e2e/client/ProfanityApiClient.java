package app.e2e.client;

import app.core.data.constant.Mode;
import app.core.data.response.FilterApiResponse;
import app.dto.request.ApiRequest;
import app.presentation.ProfanityController;
import app.test.support.fixture.SeedClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * {@link ProfanityController}의 HTTP 계약을 호출하는 E2E 테스트용 client.
 *
 * <p>URI, 인증 헤더, JSON 직렬화, 정상 응답 역직렬화를 한 곳에서 관리한다. 시나리오 검증과 비즈니스 assertion은 테스트 본문에 둔다.
 */
public final class ProfanityApiClient {

  private static final String FILTER_PATH = "/api/v1/filter";
  private static final String API_KEY_HEADER = "x-api-key";

  private final MockMvcTester mockMvcTester;
  private final ObjectMapper objectMapper;

  public static ProfanityApiClient create(MockMvcTester mockMvcTester, ObjectMapper objectMapper) {
    return new ProfanityApiClient(mockMvcTester, objectMapper);
  }

  private ProfanityApiClient(MockMvcTester mockMvcTester, ObjectMapper objectMapper) {
    this.mockMvcTester = mockMvcTester;
    this.objectMapper = objectMapper;
  }

  public ApiCallResponse<FilterApiResponse> basicProfanity(SeedClient client, ApiRequest request)
      throws Exception {
    var result =
        mockMvcTester
            .post()
            .uri(FILTER_PATH)
            .header(API_KEY_HEADER, client.apiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .exchange();

    return ApiCallResponse.of(result, objectMapper, FilterApiResponse.class);
  }

  public ApiCallResponse<FilterApiResponse> basicProfanity(
      SeedClient client, String text, Mode mode) throws Exception {
    return basicProfanity(client, ApiRequest.builder().text(text).mode(mode).build());
  }
}
