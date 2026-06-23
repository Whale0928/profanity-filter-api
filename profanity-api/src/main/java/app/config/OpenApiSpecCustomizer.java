package app.config;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import app.core.data.response.constant.StatusCode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiSpecCustomizer {

  private static final String API_RESPONSE_VOID_REF = "#/components/schemas/ApiResponseVoid";
  private static final String API_REQUEST_REF = "#/components/schemas/ApiRequest";

  @Bean
  OpenApiCustomizer profanityOpenApiCustomizer() {
    return openApi -> {
      addApiResponseVoidSchema(openApi);
      describeGeneratedApiResponseSchemas(openApi);
      customizeFilterOperation(openApi);
      customizeOperationExamples(openApi);
      customizeHealthOperations(openApi);
    };
  }

  private static void customizeFilterOperation(OpenAPI openApi) {
    Operation operation = operation(openApi, "/api/v1/filter", HttpMethod.POST);
    if (operation == null) {
      return;
    }

    operation.setOperationId("basicProfanity");
    operation.setParameters(
        operation.getParameters() == null
            ? null
            : operation.getParameters().stream()
                .filter(parameter -> !"request".equals(parameter.getName()))
                .toList());
    operation.setRequestBody(
        new RequestBody()
            .required(true)
            .description("비속어 필터링 요청 본문입니다. JSON과 form 요청은 같은 필드 계약을 사용합니다.")
            .content(
                new Content()
                    .addMediaType(
                        APPLICATION_JSON_VALUE,
                        jsonMediaType(
                            API_REQUEST_REF, "filter", "FILTER 모드 요청", filterRequestExample()))
                    .addMediaType(
                        APPLICATION_FORM_URLENCODED_VALUE,
                        jsonMediaType(
                            API_REQUEST_REF,
                            "filterForm",
                            "form FILTER 모드 요청",
                            filterRequestExample()))));
  }

  private static void customizeOperationExamples(OpenAPI openApi) {
    addExamples(
        openApi,
        "/api/v1/clients/register",
        HttpMethod.POST,
        "success",
        clientRegisterRequestExample(),
        apiResponse(clientRegisterResponseExample()));
    addExamples(
        openApi,
        "/api/v1/clients",
        HttpMethod.GET,
        "success",
        null,
        apiResponse(clientMetadataExample()));
    addExamples(
        openApi, "/api/v1/clients", HttpMethod.DELETE, "success", null, apiResponse(Boolean.TRUE));
    addExamples(
        openApi,
        "/api/v1/clients/update",
        HttpMethod.POST,
        "success",
        clientUpdateRequestExample(),
        apiResponse(clientMetadataExample()));
    addExamples(
        openApi,
        "/api/v1/clients/reissue",
        HttpMethod.POST,
        "success",
        null,
        apiResponse(map("newApiKey", "pf_sample_reissued_api_key")));
    addExamples(
        openApi,
        "/api/v1/clients/send-email",
        HttpMethod.GET,
        "success",
        null,
        apiResponse("send email"));
    addExamples(
        openApi,
        "/api/v1/clients/send-email",
        HttpMethod.PUT,
        "success",
        mailPayloadRequestExample(),
        apiResponse(map("apikey", "pf_sample_verified_api_key")));
    addExamples(
        openApi,
        "/api/v1/word/request",
        HttpMethod.POST,
        "success",
        wordRequestExample(),
        apiResponse(wordRequestResponseExample()));
    addExamples(
        openApi,
        "/api/v1/word/accept/{requestId}",
        HttpMethod.POST,
        "success",
        null,
        apiResponse(Boolean.FALSE));
    addExamples(
        openApi,
        "/api/v1/filter",
        HttpMethod.POST,
        "filter",
        filterRequestExample(),
        filterResponseExample());
    addExamples(
        openApi,
        "/api/v1/filter/advanced",
        HttpMethod.POST,
        "success",
        null,
        filterResponseExample());
    addExamples(openApi, "/api/v1/sync", HttpMethod.GET, "success", null, "SUCCESS_SYNC_WORD");
  }

  private static void customizeHealthOperations(OpenAPI openApi) {
    addPlainTextExample(openApi, "/api/v1/health", "OK");
    addPlainTextExample(openApi, "/api/v1/ping", "PONG");
  }

  private static void addExamples(
      OpenAPI openApi,
      String path,
      HttpMethod method,
      String successExampleName,
      Object requestExample,
      Object successResponseExample) {
    Operation operation = operation(openApi, path, method);
    if (operation == null) {
      return;
    }

    if (requestExample != null && operation.getRequestBody() != null) {
      Content content = operation.getRequestBody().getContent();
      if (content != null && content.get(APPLICATION_JSON_VALUE) != null) {
        content
            .get(APPLICATION_JSON_VALUE)
            .addExamples(successExampleName, example("정상 요청", requestExample));
      }
    }

    io.swagger.v3.oas.models.media.MediaType okJson = okJsonMediaType(operation);
    if (okJson == null) {
      return;
    }

    okJson.addExamples(successExampleName, example("정상 응답", successResponseExample));
    okJson.addExamples(
        "badRequest",
        example(
            "잘못된 요청",
            errorResponse(StatusCode.BAD_REQUEST, "필수 파라미터가 누락되었거나 요청 데이터 형식이 올바르지 않습니다.")));
    okJson.setSchema(oneOfWithError(okJson.getSchema()));
  }

  private static void addPlainTextExample(OpenAPI openApi, String path, String value) {
    Operation operation = operation(openApi, path, HttpMethod.GET);
    if (operation == null) {
      return;
    }
    ApiResponse ok = operation.getResponses().get("200");
    ok.setContent(
        new Content()
            .addMediaType(
                TEXT_PLAIN_VALUE,
                new io.swagger.v3.oas.models.media.MediaType()
                    .schema(new StringSchema())
                    .addExamples("success", example("정상 응답", value))));
  }

  private static io.swagger.v3.oas.models.media.MediaType okJsonMediaType(Operation operation) {
    ApiResponses responses = operation.getResponses();
    if (responses == null || responses.get("200") == null) {
      return null;
    }
    ApiResponse ok = responses.get("200");
    if (ok.getContent() == null) {
      ok.setContent(new Content());
    }
    return ok.getContent().get(APPLICATION_JSON_VALUE);
  }

  private static ComposedSchema oneOfWithError(Schema<?> successSchema) {
    ComposedSchema schema = new ComposedSchema();
    if (successSchema != null) {
      schema.addOneOfItem(successSchema);
    }
    schema.addOneOfItem(refSchema(API_RESPONSE_VOID_REF));
    return schema;
  }

  private static io.swagger.v3.oas.models.media.MediaType jsonMediaType(
      String schemaRef, String exampleName, String summary, Object exampleValue) {
    return new io.swagger.v3.oas.models.media.MediaType()
        .schema(refSchema(schemaRef))
        .addExamples(exampleName, example(summary, exampleValue));
  }

  private static void addApiResponseVoidSchema(OpenAPI openApi) {
    Schema<?> schema =
        new ObjectSchema()
            .description("성공 데이터 없이 오류 상태만 반환하는 공통 응답입니다.")
            .addProperty("status", refSchema("#/components/schemas/Status"))
            .addProperty("data", new Schema<>().nullable(true).description("오류 응답에서는 null입니다."))
            .addProperty(
                "meta",
                new ObjectSchema()
                    .description("응답 커스터마이징 메타데이터입니다. 값이 없으면 응답에서 생략됩니다.")
                    .additionalProperties(new ObjectSchema()));
    openApi.getComponents().addSchemas("ApiResponseVoid", schema);
  }

  private static void describeGeneratedApiResponseSchemas(OpenAPI openApi) {
    openApi.getComponents().getSchemas().forEach(OpenApiSpecCustomizer::describeSchema);
  }

  private static void describeSchema(String name, Schema<?> schema) {
    if (name.startsWith("ApiResponse")) {
      schema.description("공통 응답 래퍼입니다. status는 처리 결과, data는 엔드포인트별 응답 데이터입니다.");
      describeProperty(schema, "status", "비즈니스 상태 코드와 설명입니다.");
      describeProperty(schema, "data", "엔드포인트별 응답 데이터입니다. 오류 응답에서는 null일 수 있습니다.");
      describeProperty(schema, "meta", "응답 커스터마이징 메타데이터입니다. 값이 없으면 응답에서 생략됩니다.");
    }
    if ("Status".equals(name)) {
      schema.description("API 처리 상태입니다.");
      describeProperty(schema, "code", "서비스 내부 비즈니스 상태 코드입니다.");
      describeProperty(schema, "message", "서비스 내부 상태 이름입니다.");
      describeProperty(schema, "description", "상태 코드에 대한 기본 설명입니다.");
      describeProperty(schema, "DetailDescription", "요청별 상세 오류 설명입니다. 상세 내용이 없으면 빈 문자열입니다.");
    }
    if ("Detected".equals(name)) {
      schema.description("필터에서 검출된 단어 정보입니다.");
      describeProperty(schema, "length", "검출된 단어의 문자 길이입니다.");
      describeProperty(schema, "filteredWord", "검출된 원문 단어입니다.");
    }
    if ("FilterApiResponse".equals(name)) {
      schema.description("비속어 필터링 응답입니다.");
      describeProperty(schema, "trackingId", "요청 추적 ID입니다.");
      describeProperty(schema, "status", "필터링 처리 상태입니다.");
      describeProperty(schema, "detected", "검출된 단어 목록입니다. 검출 결과가 없으면 빈 배열입니다.");
      describeProperty(schema, "filtered", "FILTER 또는 advanced 처리 후 마스킹된 문장입니다.");
      describeProperty(schema, "elapsed", "필터 처리 소요 시간입니다.");
    }
    if ("ClientMetadata".equals(name)) {
      schema.description("클라이언트 메타데이터입니다.");
      describeProperty(schema, "id", "클라이언트 식별자입니다.");
      describeProperty(schema, "email", "API Key 발급에 사용한 이메일입니다.");
      describeProperty(schema, "issuerInfo", "API Key 발급자 정보입니다.");
      describeProperty(schema, "note", "클라이언트 메모입니다.");
      describeProperty(schema, "permissions", "클라이언트 권한 목록입니다.");
      describeProperty(schema, "issuedAt", "API Key 발급 시각입니다.");
    }
    if ("ClientsRegistResponse".equals(name)) {
      schema.description("신규 클라이언트 등록 결과입니다.");
      describeProperty(schema, "name", "등록된 이름 또는 조직명입니다.");
      describeProperty(schema, "email", "API Key 발급에 사용한 이메일입니다.");
      describeProperty(schema, "apiKey", "발급된 API Key입니다.");
      describeProperty(schema, "note", "등록 메모입니다.");
    }
    if ("ApiKeyReissueResponse".equals(name)) {
      schema.description("API Key 재발급 결과입니다.");
      describeProperty(schema, "newApiKey", "새로 발급된 API Key입니다.");
    }
    if ("EmailVerificationResponse".equals(name)) {
      schema.description("이메일 인증 코드 검증 결과입니다.");
      describeProperty(schema, "apikey", "인증된 이메일에 연결된 API Key입니다.");
    }
    if ("MessageResponse".equals(name)) {
      schema.description("요청 처리 결과 메시지입니다.");
      describeProperty(schema, "result", "요청 처리 성공 여부입니다.");
      describeProperty(schema, "message", "사용자에게 전달할 다국어 메시지입니다.");
    }
    if ("BusinessMessage".equals(name)) {
      schema.description("사용자에게 전달할 다국어 비즈니스 메시지입니다.");
      describeProperty(schema, "engMessage", "영문 메시지입니다.");
      describeProperty(schema, "korMessage", "국문 메시지입니다.");
      describeProperty(schema, "result", "비즈니스 처리 성공 여부입니다.");
    }
  }

  private static void describeProperty(Schema<?> schema, String propertyName, String description) {
    if (schema.getProperties() == null || schema.getProperties().get(propertyName) == null) {
      return;
    }
    schema.getProperties().get(propertyName).description(description);
  }

  private static Operation operation(OpenAPI openApi, String path, HttpMethod method) {
    if (openApi.getPaths() == null || openApi.getPaths().get(path) == null) {
      return null;
    }
    return switch (method) {
      case GET -> openApi.getPaths().get(path).getGet();
      case POST -> openApi.getPaths().get(path).getPost();
      case PUT -> openApi.getPaths().get(path).getPut();
      case DELETE -> openApi.getPaths().get(path).getDelete();
    };
  }

  private static Schema<?> refSchema(String ref) {
    return new Schema<>().$ref(ref);
  }

  private static Example example(String summary, Object value) {
    return new Example().summary(summary).value(value);
  }

  private static Map<String, Object> apiResponse(Object data) {
    return map("status", status(StatusCode.OK, ""), "data", data);
  }

  private static Map<String, Object> errorResponse(StatusCode code, String detailDescription) {
    return map("status", status(code, detailDescription), "data", null);
  }

  private static Map<String, Object> status(StatusCode code, String detailDescription) {
    return map(
        "code", code.code(),
        "message", code.status(),
        "description", code.description(),
        "DetailDescription", detailDescription);
  }

  private static Map<String, Object> clientRegisterRequestExample() {
    return map(
        "name", "샘플 프로젝트",
        "email", "user@example.com",
        "issuerInfo", "비속어 필터링 연동",
        "note", "검증 환경에서 사용");
  }

  private static Map<String, Object> clientRegisterResponseExample() {
    return map(
        "name", "샘플 프로젝트",
        "email", "user@example.com",
        "apiKey", "pf_sample_issued_api_key",
        "note", "검증 환경에서 사용");
  }

  private static Map<String, Object> clientMetadataExample() {
    return map(
        "id", "018f4fd8-9f6f-7d1a-9b80-3f1f8dd7c001",
        "email", "user@example.com",
        "issuerInfo", "비속어 필터링 연동",
        "note", "운영 환경에서 사용",
        "permissions", List.of("READ"),
        "issuedAt", "2026-06-23T09:00:00");
  }

  private static Map<String, Object> clientUpdateRequestExample() {
    return map("issuerInfo", "비속어 필터링 연동", "note", "운영 환경에서 사용");
  }

  private static Map<String, Object> mailPayloadRequestExample() {
    return map("email", "user@example.com", "code", "123456");
  }

  private static Map<String, Object> wordRequestExample() {
    return map(
        "word", "나쁜말샘플",
        "reason", "서비스 정책상 필터링이 필요합니다.",
        "severity", "MEDIUM",
        "type", "ADD");
  }

  private static Map<String, Object> wordRequestResponseExample() {
    return map(
        "result",
        true,
        "message",
        map("engMessage", "Requested successfully", "korMessage", "요청에 성공했습니다", "result", true));
  }

  private static Map<String, Object> filterRequestExample() {
    return map("text", "문장 안에 나쁜말샘플 이 포함된다", "mode", "FILTER");
  }

  private static Map<String, Object> filterResponseExample() {
    return map(
        "trackingId",
        "018f4fd8-9f6f-7d1a-9b80-3f1f8dd7c002",
        "status",
        status(StatusCode.OK, ""),
        "detected",
        List.of(map("length", 5, "filteredWord", "나쁜말샘플")),
        "filtered",
        "문장 안에 ***** 이 포함된다",
        "elapsed",
        "0.00000000 s / 0.00000 ms / 0.000 µs");
  }

  private static Map<String, Object> map(Object... values) {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put((String) values[i], values[i + 1]);
    }
    return map;
  }

  private enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE
  }
}
