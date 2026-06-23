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

  @Test
  @DisplayName("주요 API 200 응답 스키마를 구체적으로 반환한다")
  void openapiJson_whenPublicApiHasSuccessResponse_returnsConcreteSuccessResponseSchemas()
      throws Exception {
    // when
    var response = mockMvcTester.get().uri("/openapi.json").exchange();

    // then
    assertThat(response).hasStatusOk();

    JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
    for (OperationPath operationPath :
        new OperationPath[] {
          new OperationPath("/paths/~1api~1v1~1clients/get/responses/200/content"),
          new OperationPath("/paths/~1api~1v1~1clients/delete/responses/200/content"),
          new OperationPath("/paths/~1api~1v1~1clients~1register/post/responses/200/content"),
          new OperationPath("/paths/~1api~1v1~1clients~1update/post/responses/200/content"),
          new OperationPath("/paths/~1api~1v1~1clients~1reissue/post/responses/200/content"),
          new OperationPath("/paths/~1api~1v1~1clients~1send-email/get/responses/200/content"),
          new OperationPath("/paths/~1api~1v1~1clients~1send-email/put/responses/200/content"),
          new OperationPath("/paths/~1api~1v1~1filter/post/responses/200/content"),
          new OperationPath("/paths/~1api~1v1~1filter~1advanced/post/responses/200/content"),
          new OperationPath("/paths/~1api~1v1~1sync/get/responses/200/content"),
          new OperationPath(
              "/paths/~1api~1v1~1word~1accept~1{requestId}/post/responses/200/content")
        }) {
      JsonNode responseContent = body.at(operationPath.pointer());
      assertThat(responseContent.has("application/json"))
          .as(operationPath.pointer() + " must have application/json content")
          .isTrue();
      assertThat(responseContent.at("/*~1*/schema/type").asText())
          .as(operationPath.pointer() + " must not remain wildcard object schema")
          .isNotEqualTo("object");

      JsonNode schema = responseContent.path("application/json").path("schema");
      assertThat(schema.isMissingNode()).as(operationPath.pointer()).isFalse();
      assertThat(schema.isEmpty()).as(operationPath.pointer()).isFalse();
    }
  }

  @Test
  @DisplayName("필터 API는 JSON과 form 요청 계약을 모두 정확하게 반환한다")
  void openapiJson_whenFilterSupportsJsonAndForm_returnsBothRequestContracts() throws Exception {
    // when
    var response = mockMvcTester.get().uri("/openapi.json").exchange();

    // then
    assertThat(response).hasStatusOk();

    JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
    JsonNode operation = body.at("/paths/~1api~1v1~1filter/post");
    assertThat(operation.at("/requestBody/content/application~1json/schema/$ref").asText())
        .isEqualTo("#/components/schemas/ApiRequest");
    assertThat(
            operation
                .at("/requestBody/content/application~1x-www-form-urlencoded/schema/$ref")
                .asText())
        .isEqualTo("#/components/schemas/ApiRequest");
    assertThat(operation.path("parameters").findValuesAsText("name")).doesNotContain("request");
  }

  @Test
  @DisplayName("주요 API는 입력과 출력 예시를 논리적으로 연결해 반환한다")
  void openapiJson_whenExamplesProvided_returnsScenarioConsistentExamples() throws Exception {
    // when
    var response = mockMvcTester.get().uri("/openapi.json").exchange();

    // then
    assertThat(response).hasStatusOk();

    JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
    JsonNode registerRequest =
        body.at(
            "/paths/~1api~1v1~1clients~1register/post/requestBody/content/application~1json/examples/success/value");
    JsonNode registerResponse =
        body.at(
            "/paths/~1api~1v1~1clients~1register/post/responses/200/content/application~1json/examples/success/value");
    assertThat(registerRequest.path("email").asText()).isEqualTo("user@example.com");
    assertThat(registerResponse.at("/data/email").asText())
        .isEqualTo(registerRequest.path("email").asText());
    assertThat(registerResponse.at("/data/apiKey").asText()).isNotBlank();

    JsonNode filterRequest =
        body.at(
            "/paths/~1api~1v1~1filter/post/requestBody/content/application~1json/examples/filter/value");
    JsonNode filterResponse =
        body.at(
            "/paths/~1api~1v1~1filter/post/responses/200/content/application~1json/examples/filter/value");
    assertThat(filterRequest.path("mode").asText()).isEqualTo("FILTER");
    assertThat(filterResponse.at("/status/code").asInt()).isEqualTo(2000);
    assertThat(filterResponse.at("/filtered").asText()).contains("*");
    assertThat(filterResponse.at("/detected/0/filteredWord").asText()).isEqualTo("나쁜말샘플");
  }

  @Test
  @DisplayName("주요 API는 HTTP 200 응답 안에 공통 오류 응답 가능성과 예시를 반환한다")
  void openapiJson_whenApiCanFail_returnsCommonErrorExamplesInOkResponse() throws Exception {
    // when
    var response = mockMvcTester.get().uri("/openapi.json").exchange();

    // then
    assertThat(response).hasStatusOk();

    JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
    for (OperationPath operationPath :
        new OperationPath[] {
          new OperationPath("/paths/~1api~1v1~1clients/get"),
          new OperationPath("/paths/~1api~1v1~1clients/delete"),
          new OperationPath("/paths/~1api~1v1~1clients~1register/post"),
          new OperationPath("/paths/~1api~1v1~1clients~1update/post"),
          new OperationPath("/paths/~1api~1v1~1clients~1reissue/post"),
          new OperationPath("/paths/~1api~1v1~1clients~1send-email/get"),
          new OperationPath("/paths/~1api~1v1~1clients~1send-email/put"),
          new OperationPath("/paths/~1api~1v1~1filter/post"),
          new OperationPath("/paths/~1api~1v1~1filter~1advanced/post"),
          new OperationPath("/paths/~1api~1v1~1sync/get"),
          new OperationPath("/paths/~1api~1v1~1word~1request/post"),
          new OperationPath("/paths/~1api~1v1~1word~1accept~1{requestId}/post")
        }) {
      JsonNode okContent =
          body.at(operationPath.pointer() + "/responses/200/content/application~1json");
      assertThat(okContent.at("/schema/oneOf").findValuesAsText("$ref"))
          .as(operationPath.pointer() + " error schema alternative")
          .contains("#/components/schemas/ApiResponseVoid");
      assertThat(okContent.at("/examples/badRequest/value/status/code").asInt())
          .as(operationPath.pointer() + " bad request example")
          .isEqualTo(4000);
    }
  }

  @Test
  @DisplayName("응답 모델은 Scalar 모델 섹션에 표시할 설명을 가진다")
  void openapiJson_whenResponseSchemasRendered_returnsDescribedResponseProperties()
      throws Exception {
    // when
    var response = mockMvcTester.get().uri("/openapi.json").exchange();

    // then
    assertThat(response).hasStatusOk();

    JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
    assertThat(
            body.at("/components/schemas/FilterApiResponse/properties/trackingId/description")
                .asText())
        .isNotBlank();
    assertThat(body.at("/components/schemas/Detected/properties/filteredWord/description").asText())
        .isNotBlank();
    assertThat(
            body.at("/components/schemas/Status/properties/DetailDescription/description").asText())
        .isNotBlank();
    assertThat(
            body.at("/components/schemas/ApiResponseClientMetadata/properties/status/description")
                .asText())
        .isNotBlank();
  }

  private record OperationPath(String pointer) {}
}
