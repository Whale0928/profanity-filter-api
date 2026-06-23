package app.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class LlmDocumentOpenApi {

  private LlmDocumentOpenApi() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "llm-docs", description = "LLM 문서 색인")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "LLM 문서 색인 조회",
      description = "`/llms.txt` 표준 경로와 `/llm.txt` 호환 경로에서 같은 LLM 문서 색인을 반환합니다.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "LLM 문서 색인 반환",
            content =
                @Content(
                    mediaType = "text/plain",
                    examples =
                        @ExampleObject(
                            name = "llms",
                            value =
                                """
                                # Profanity Filter API

                                AI agents and non-JavaScript clients can use these documents:

                                - OpenAPI JSON: /openapi.json
                                - Overview: /openapi/overview.md
                                - Error model: /openapi/error-model.md
                                - Authentication: /openapi/authentication.md
                                """)))
      })
  public @interface GetIndex {}
}
