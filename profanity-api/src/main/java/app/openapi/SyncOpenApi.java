package app.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class SyncOpenApi {
  private SyncOpenApi() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "Sync", description = "비속어 데이터 동기화 API")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "비속어 데이터 동기화",
      description = "관리 비밀번호로 비속어 데이터를 동기화합니다.",
      parameters = @Parameter(name = "password", description = "동기화 관리 비밀번호", required = true))
  public @interface DoSync {}
}
