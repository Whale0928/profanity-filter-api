package app.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenApiSpecE2ETest extends AbstractApiTester {

  @Test
  @DisplayName("OpenAPI JSON 스펙을 반환한다")
  void openapiJson_whenRequested_returnsOpenApiSpec() throws Exception {
    // when
    var response = mockMvcTester.get().uri("/openapi.json").exchange();

    // then
    assertThat(response).hasStatusOk();

    JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
    assertThat(body.at("/openapi").asText()).isNotBlank();
    assertThat(body.at("/info/title").asText()).isEqualTo("Profanity Filter API");
    assertThat(body.at("/components/securitySchemes/ApiKeyAuth/name").asText())
        .isEqualTo("x-api-key");
    assertThat(body.at("/paths/~1api~1v1~1filter/post").isMissingNode()).isFalse();
    assertThat(body.at("/paths/~1api~1v1~1clients~1register/post").isMissingNode()).isFalse();
    assertThat(body.at("/paths/~1api~1v1~1health/get").isMissingNode()).isFalse();
    assertThat(body.at("/paths/~1swagger-ui~1index.html/get").isMissingNode()).isTrue();
  }

  @Test
  @DisplayName("단어 변경 요청 200 응답 스키마를 반환한다")
  void wordRequestOpenapi_whenRequested_returnsConcreteSuccessResponseSchema() throws Exception {
    // when
    var response = mockMvcTester.get().uri("/openapi.json").exchange();

    // then
    assertThat(response).hasStatusOk();

    JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
    JsonNode responseContent =
        body.at("/paths/~1api~1v1~1word~1request/post/responses/200/content");
    assertThat(responseContent.has("application/json")).isTrue();
    assertThat(responseContent.at("/*~1*/schema/type").asText()).isNotEqualTo("object");

    JsonNode schema = responseContent.path("application/json").path("schema");
    assertThat(schema.isMissingNode()).isFalse();
    assertThat(schema.isEmpty()).isFalse();
  }
}
