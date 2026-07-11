package app.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class OpenApiSpecE2ETest extends AbstractApiTester {

  private static final List<ClassPathResource> OVERVIEW_RESOURCES =
      List.of(
          new ClassPathResource("openapi/overview.md"),
          new ClassPathResource("openapi/line.md"),
          new ClassPathResource("openapi/error-model.md"),
          new ClassPathResource("openapi/line.md"),
          new ClassPathResource("openapi/authentication.md"));

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
    assertThat(body.at("/info/description").asText())
        .as("OpenAPI JSON은 API 계약 설명만 담고 overview 본문은 /overview.md로 분리한다")
        .isEqualTo("한국어와 영어 비속어를 검출하고 필터링하는 API입니다.");
    String description = body.at("/info/description").asText();
    assertThat(description).doesNotContain("# Profanity Filter API");
    assertThat(description).doesNotContain("# Error Model", "# Authentication");
    assertThat(body.at("/components/securitySchemes/ApiKeyAuth/name").asText())
        .isEqualTo("x-api-key");
    assertThat(body.at("/components/securitySchemes/ApiKeyAuth/description").asText())
        .as("ApiKeyAuth 보안 스키마는 x-api-key 헤더 설명을 제공해야 한다")
        .isEqualTo("클라이언트 등록 후 발급받은 API Key");
    assertThat(body.at("/components/securitySchemes/LoginJwtAuth/type").asText()).isEqualTo("http");
    assertThat(body.at("/components/securitySchemes/LoginJwtAuth/scheme").asText())
        .isEqualTo("bearer");
    assertThat(body.at("/components/securitySchemes/LoginJwtAuth/bearerFormat").asText())
        .isEqualTo("JWT");
    assertThat(body.at("/paths/~1api~1v1~1filter/post").isMissingNode()).isFalse();
    assertThat(body.at("/paths/~1api~1v1~1clients~1register/post").isMissingNode()).isFalse();
    assertThat(body.at("/paths/~1api~1v1~1auth~1exchange/post").isMissingNode()).isFalse();
    assertThat(body.at("/paths/~1api~1v1~1auth~1csrf/get").isMissingNode()).isFalse();
    assertThat(body.at("/paths/~1api~1v1~1auth~1refresh/post").isMissingNode()).isFalse();
    assertThat(body.at("/paths/~1api~1v1~1auth~1me/get/security/0/LoginJwtAuth").isArray())
        .isTrue();
    assertThat(body.at("/paths/~1api~1v1~1health/get").isMissingNode()).isFalse();
    assertThat(body.at("/paths/~1overview.md/get").isMissingNode()).isTrue();
    assertThat(body.at("/paths/~1llms.txt/get").isMissingNode()).isTrue();
    assertThat(body.at("/paths/~1swagger-ui~1index.html/get").isMissingNode()).isTrue();
  }

  @Test
  @DisplayName("LLM 문서 색인과 OpenAPI Markdown 문서를 인증 없이 반환한다")
  void llmsAndOpenApiMarkdown_whenRequested_returnsPublicDocuments() throws Exception {
    // when
    var llmsResponse = mockMvcTester.get().uri("/llms.txt").exchange();
    var llmAliasResponse = mockMvcTester.get().uri("/llm.txt").exchange();
    var overviewResponse = mockMvcTester.get().uri("/overview.md").exchange();

    // then
    assertThat(llmsResponse).hasStatusOk();
    assertThat(llmsResponse.getResponse().getContentAsString())
        .contains("/openapi.json", "/overview.md");
    assertThat(llmAliasResponse).hasStatusOk();
    assertThat(llmAliasResponse.getResponse().getContentAsString())
        .as("/llm.txt alias는 /llms.txt와 같은 LLM 문서 색인을 반환해야 한다")
        .isEqualTo(llmsResponse.getResponse().getContentAsString());

    assertThat(overviewResponse).hasStatusOk();
    String overview = overviewResponse.getResponse().getContentAsString();
    assertThat(overview).isEqualTo(readMarkdownInOrder(OVERVIEW_RESOURCES));
    assertThat(overview).as("문서 조각 사이에는 Markdown 구분선이 포함되어야 한다").contains("----------");
    assertThat(overview.indexOf("# Error Model"))
        .as("/overview.md는 overview, error-model, authentication 순서로 조합되어야 한다")
        .isPositive();
    assertThat(overview.indexOf("# Error Model"))
        .as("error-model은 authentication보다 먼저 포함되어야 한다")
        .isLessThan(overview.indexOf("# Authentication"));
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
          new OperationPath("/paths/~1api~1v1~1auth~1exchange/post/responses/200/content"),
          new OperationPath("/paths/~1api~1v1~1auth~1csrf/get/responses/200/content"),
          new OperationPath("/paths/~1api~1v1~1auth~1refresh/post/responses/200/content"),
          new OperationPath("/paths/~1api~1v1~1auth~1me/get/responses/200/content"),
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
  @DisplayName("필터 API는 문서 어노테이션의 요청과 응답 예제를 반환한다")
  void openapiJson_whenFilterOpenApiAnnotationProvided_returnsRequestAndResponseExamples()
      throws Exception {
    // when
    var response = mockMvcTester.get().uri("/openapi.json").exchange();

    // then
    assertThat(response).hasStatusOk();

    JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
    JsonNode operation = body.at("/paths/~1api~1v1~1filter/post");
    assertThat(operation.path("summary").asText()).isEqualTo("비속어 필터링 요청");
    assertThat(operation.at("/security/0/ApiKeyAuth").isArray()).isTrue();
    assertThat(operation.at("/requestBody/content/application~1json/schema/$ref").asText())
        .isEqualTo("#/components/schemas/ApiRequest");
    assertThat(
            operation
                .at("/requestBody/content/application~1x-www-form-urlencoded/schema/$ref")
                .asText())
        .isEqualTo("#/components/schemas/ApiRequest");
    assertThat(
            operation
                .at("/requestBody/content/application~1json/examples/filter/value/mode")
                .asText())
        .isEqualTo("FILTER");
    assertThat(
            operation
                .at("/requestBody/content/application~1json/examples/async/value/callbackUrl")
                .asText())
        .isEqualTo("https://example.com/filter/callback");
    assertThat(
            operation
                .at("/responses/200/content/application~1json/examples/filter/value/status/code")
                .asInt())
        .isEqualTo(2000);
    assertThat(
            operation
                .at(
                    "/responses/200/content/application~1json/examples/asyncAccepted/value/status/code")
                .asInt())
        .isEqualTo(2020);
    assertThat(operation.path("parameters").findValuesAsText("name")).doesNotContain("request");

    JsonNode advancedOperation = body.at("/paths/~1api~1v1~1filter~1advanced/post");
    assertThat(advancedOperation.path("summary").asText()).isEqualTo("고급 비속어 필터링 요청");
    assertThat(advancedOperation.at("/security/0/ApiKeyAuth").isArray()).isTrue();
    JsonNode advancedParameters = advancedOperation.path("parameters");
    JsonNode wordParameter = findParameter(advancedParameters, "word", "query");
    assertThat(wordParameter.isMissingNode())
        .as("advanced API는 검사 대상 word query 파라미터를 문서화해야 한다")
        .isFalse();
    assertThat(wordParameter.path("name").asText())
        .as("advanced API query 파라미터 이름은 word여야 한다")
        .isEqualTo("word");
    assertThat(wordParameter.path("in").asText())
        .as("word는 query 파라미터로 렌더링되어야 한다")
        .isEqualTo("query");
    assertThat(
            advancedOperation
                .at("/responses/200/content/application~1json/examples/filter/value/filtered")
                .asText())
        .isEqualTo("advanced *****");
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
    assertThat(body.at("/components/schemas/ClientMetadata/properties/email/example").asText())
        .isEqualTo("user@example.com");
    assertThat(
            body.at("/components/schemas/ClientsRegistResponse/properties/apiKey/example").asText())
        .isEqualTo("pf_sample_issued_api_key");
  }

  private record OperationPath(String pointer) {}

  private static JsonNode findParameter(JsonNode parameters, String name, String in) {
    if (!parameters.isArray()) {
      return MissingNode.getInstance();
    }
    for (JsonNode parameter : parameters) {
      if (name.equals(parameter.path("name").asText())
          && in.equals(parameter.path("in").asText())) {
        return parameter;
      }
    }
    return MissingNode.getInstance();
  }

  private static String readMarkdownInOrder(List<ClassPathResource> resources) {
    return resources.stream()
        .map(OpenApiSpecE2ETest::readResource)
        .reduce((left, right) -> left + "\n\n" + right)
        .orElse("");
  }

  private static String readResource(ClassPathResource resource) {
    try (var inputStream = resource.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception exception) {
      throw new IllegalStateException("테스트 문서 리소스를 읽을 수 없습니다: " + resource, exception);
    }
  }
}
