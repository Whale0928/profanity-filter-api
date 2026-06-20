package app.e2e;

import static app.test.support.fixture.IntegrationClients.READ_CLIENT;
import static app.test.support.fixture.IntegrationWords.ACTIVE_PROFANITY_SAMPLE;
import static org.assertj.core.api.Assertions.assertThat;

import app.core.data.constant.Mode;
import app.core.data.response.FilterApiResponse;
import app.dto.request.ApiRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ProfanityFilterE2ETest extends AbstractApiTester {

  @Test
  @DisplayName("인증된 클라이언트가 DB 단어 기준으로 필터링하고 요청 이력을 저장한다")
  void filter_whenAuthenticatedClientRequestsWithSeededWord_masksTextAndRecordsTracking()
      throws Exception {
    // given
    String requestText = "문장 안에 " + ACTIVE_PROFANITY_SAMPLE.word() + " 이 포함된다";

    // when
    ApiRequest request = ApiRequest.builder().text(requestText).mode(Mode.FILTER).build();
    var result =
        mockMvcTester
            .post()
            .uri("/api/v1/filter")
            .header("x-api-key", READ_CLIENT.apiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .exchange();

    assertThat(result).hasStatusOk();

    FilterApiResponse response =
        objectMapper.readValue(result.getResponse().getContentAsString(), FilterApiResponse.class);

    // then
    assertThat(response.trackingId()).isNotNull();
    assertThat(response.status().code()).isEqualTo(2000);
    assertThat(response.detected())
        .anyMatch(detected -> detected.filteredWord().equals(ACTIVE_PROFANITY_SAMPLE.word()));
    assertThat(response.filtered()).isEqualTo("문장 안에 ***** 이 포함된다");
    recordProbe.assertFilterRecord(READ_CLIENT, requestText, ACTIVE_PROFANITY_SAMPLE);
  }
}
