package app.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import app.e2e.client.ApiCallResponse;
import app.e2e.client.HealthApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HealthE2ETest extends AbstractApiTester {

  private HealthApiClient healthApi;

  @BeforeEach
  void setUpHealthE2ETest() {
    healthApi = HealthApiClient.create(mockMvcTester, objectMapper);
  }

  @Test
  @DisplayName("헬스 체크 응답을 반환한다")
  void health_whenRequested_returnsOk() throws Exception {
    // when
    ApiCallResponse<String> response = healthApi.health();

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(response.body()).isEqualTo("OK");
  }

  @Test
  @DisplayName("핑 체크 응답을 반환한다")
  void ping_whenRequested_returnsPong() throws Exception {
    // when
    ApiCallResponse<String> response = healthApi.ping();

    // then
    assertThat(response.result()).hasStatusOk();
    assertThat(response.body()).isEqualTo("PONG");
  }
}
