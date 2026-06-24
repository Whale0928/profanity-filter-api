package app.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class HealthOpenApi {
  private HealthOpenApi() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "Health", description = "서버 상태 확인 API")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(summary = "헬스 체크", description = "서버 상태를 확인합니다. 정상 상태이면 OK를 반환합니다.")
  public @interface Health {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(summary = "핑 체크", description = "서버 응답 상태를 확인합니다. 정상 상태이면 PONG을 반환합니다.")
  public @interface Ping {}
}
