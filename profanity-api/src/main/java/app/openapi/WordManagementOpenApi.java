package app.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class WordManagementOpenApi {
  private WordManagementOpenApi() {}

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

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "단어 변경 요청 승인",
      description = "WRITE 권한을 가진 클라이언트가 단어 변경 요청을 승인합니다.",
      parameters = @Parameter(name = "requestId", description = "승인할 요청 ID 목록", required = true),
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  public @interface AcceptWord {}
}
