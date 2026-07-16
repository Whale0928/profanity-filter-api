package app.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class WordManagementOpenApi {
  private WordManagementOpenApi() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "Word Management", description = "비속어 단어 추가, 삭제, 수정 요청 API")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "단어 변경 요청",
      description =
          """
          비속어 단어의 추가, 삭제, 수정을 요청합니다.
          severity는 LOW, MEDIUM, HIGH를 사용하고 type은 ADD, REMOVE, MODIFY를 사용합니다.
          """,
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  public @interface RequestNewWord {}
}
