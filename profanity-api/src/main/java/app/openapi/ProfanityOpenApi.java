package app.openapi;

import app.core.data.response.FilterApiResponse;
import app.dto.request.ApiRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;

public final class ProfanityOpenApi {
  private ProfanityOpenApi() {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "비속어 필터링 요청",
      description =
          """
          클라이언트 등록 후 발급받은 API Key로 비속어 검사를 요청합니다.
          QUICK은 원색적인 표현을 간략히 검증하고, NORMAL은 데이터베이스의 모든 비속어를 검증하며,
          FILTER는 검출된 단어를 마스킹해 반환합니다.

          callbackUrl을 전달하면 요청을 즉시 접수하고 같은 trackingId로 비동기 필터링 결과를 callbackUrl에 전달합니다.
          """,
      requestBody =
          @RequestBody(
              required = true,
              description =
                  """
                  필터링할 문장과 처리 모드를 전달합니다.
                  JSON 요청과 form-urlencoded 요청은 같은 필드 계약을 사용합니다.
                  """,
              content = {
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiRequest.class),
                    examples = {
                      @ExampleObject(
                          name = "filter",
                          summary = "FILTER 모드로 감지 단어를 마스킹",
                          value =
                              """
                              {
                                "text": "문장 안에 나쁜말샘플 이 포함된다",
                                "mode": "FILTER"
                              }
                              """),
                      @ExampleObject(
                          name = "quick",
                          summary = "QUICK 모드로 첫 번째 감지 단어만 확인",
                          value =
                              """
                              {
                                "text": "문장 안에 비속어샘플 과 나쁜말샘플 이 포함된다",
                                "mode": "QUICK"
                              }
                              """),
                      @ExampleObject(
                          name = "async",
                          summary = "비동기 필터링 요청",
                          value =
                              """
                              {
                                "text": "문장 안에 나쁜말샘플 이 포함된다",
                                "mode": "FILTER",
                                "callbackUrl": "https://example.com/filter/callback"
                              }
                              """)
                    }),
                @Content(
                    mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                    schema = @Schema(implementation = ApiRequest.class),
                    examples =
                        @ExampleObject(
                            name = "form",
                            summary = "form-urlencoded 요청",
                            value = "text=문장 안에 나쁜말샘플 이 포함된다&mode=FILTER"))
              }),
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "비속어 필터링 처리 결과",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = FilterApiResponse.class),
                      examples = {
                        @ExampleObject(
                            name = "filter",
                            summary = "FILTER 모드 응답",
                            value =
                                """
                                {
                                  "trackingId": "018f4fd8-9f6f-7d1a-9b80-3f1f8dd7c002",
                                  "status": {
                                    "code": 2000,
                                    "message": "Ok",
                                    "description": "정상적으로 처리 되었습니다.",
                                    "DetailDescription": ""
                                  },
                                  "detected": [
                                    {
                                      "length": 5,
                                      "filteredWord": "나쁜말샘플"
                                    }
                                  ],
                                  "filtered": "문장 안에 ***** 이 포함된다",
                                  "elapsed": "0.00000000 s / 0.00000 ms / 0.000 µs"
                                }
                                """),
                        @ExampleObject(
                            name = "normal",
                            summary = "NORMAL 모드 응답",
                            value =
                                """
                                {
                                  "trackingId": "018f4fd8-9f6f-7d1a-9b80-3f1f8dd7c002",
                                  "status": {
                                    "code": 2000,
                                    "message": "Ok",
                                    "description": "정상적으로 처리 되었습니다.",
                                    "DetailDescription": ""
                                  },
                                  "detected": [
                                    {
                                      "length": 5,
                                      "filteredWord": "나쁜말샘플"
                                    },
                                    {
                                      "length": 5,
                                      "filteredWord": "비속어샘플"
                                    }
                                  ],
                                  "filtered": "",
                                  "elapsed": "0.00000000 s / 0.00000 ms / 0.000 µs"
                                }
                                """),
                        @ExampleObject(
                            name = "asyncAccepted",
                            summary = "비동기 요청 접수 응답",
                            value =
                                """
                                {
                                  "trackingId": "018f4fd8-9f6f-7d1a-9b80-3f1f8dd7c002",
                                  "status": {
                                    "code": 2020,
                                    "message": "Accepted",
                                    "description": "요청이 접수 되었습니다. 처리가 완료 시 결과를 받을 수 확인할 수 있습니다.",
                                    "DetailDescription": ""
                                  },
                                  "detected": [],
                                  "filtered": "",
                                  "elapsed": "0.00000000 s / 0.00000 ms / 0.000 µs"
                                }
                                """)
                      })),
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  public @interface BasicProfanity {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "비속어 필터링 요청 form",
      description = "application/x-www-form-urlencoded 형식으로 비속어 검사를 요청합니다.",
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  public @interface BasicProfanityForm {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "고급 비속어 필터링 요청",
      description =
          """
          word 쿼리 파라미터로 전달한 문장을 FILTER 모드처럼 마스킹합니다.
          단순 쿼리 파라미터 기반 연동이 필요한 클라이언트를 위한 보조 엔드포인트입니다.
          """,
      parameters = @Parameter(name = "word", description = "검사할 단어", required = true),
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "고급 비속어 필터링 처리 결과",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = FilterApiResponse.class),
                      examples =
                          @ExampleObject(
                              name = "filter",
                              summary = "고급 필터링 응답",
                              value =
                                  """
                                  {
                                    "trackingId": "018f4fd8-9f6f-7d1a-9b80-3f1f8dd7c003",
                                    "status": {
                                      "code": 2000,
                                      "message": "Ok",
                                      "description": "정상적으로 처리 되었습니다.",
                                      "DetailDescription": ""
                                    },
                                    "detected": [
                                      {
                                        "length": 5,
                                        "filteredWord": "나쁜말샘플"
                                      }
                                    ],
                                    "filtered": "advanced *****",
                                    "elapsed": "0.00000000 s / 0.00000 ms / 0.000 µs"
                                  }
                                  """))),
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  public @interface AdvancedProfanity {}
}
