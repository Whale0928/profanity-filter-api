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

public final class OpenApiMarkdownOpenApi {

  private OpenApiMarkdownOpenApi() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "openapi-docs", description = "OpenAPI Markdown 보조 문서")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "OpenAPI Markdown 문서 조회",
      description = "LLM과 비 JavaScript 클라이언트가 읽을 수 있는 Markdown 문서를 반환합니다.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Markdown 문서 반환",
            content =
                @Content(
                    mediaType = "text/markdown",
                    examples =
                        @ExampleObject(
                            name = "overview",
                            value =
                                """
                                한국어 비속어 필터링 API는 문장 안의 부적절한 표현을 감지하고,
                                필요한 경우 검출된 단어를 마스킹해 반환합니다.
                                """))),
        @ApiResponse(responseCode = "404", description = "문서를 찾을 수 없음")
      })
  public @interface GetMarkdown {}
}
