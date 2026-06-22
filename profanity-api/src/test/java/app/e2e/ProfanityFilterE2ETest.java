package app.e2e;

import static app.test.support.fixture.SeedClients.READ_CLIENT;
import static app.test.support.fixture.SeedWords.ACTIVE_PROFANITY_SAMPLE;
import static org.assertj.core.api.Assertions.assertThat;

import app.core.data.constant.Mode;
import app.core.data.response.FilterApiResponse;
import app.e2e.client.ApiCallResponse;
import app.e2e.client.ProfanityApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
