package app.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ApiKeysOpenApi {
  private ApiKeysOpenApi() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "API Keys", description = "SSO 로그인 사용자의 API Key 관리 API")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "내 API Key 목록 조회",
      description = "현재 로그인 사용자가 소유한 활성·만료 API Key를 조회합니다. 키 원문은 반환하지 않습니다.",
      security = @SecurityRequirement(name = "LoginJwtAuth"))
  public @interface ListKeys {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "API Key 발급",
      description = "SSO 대표 이메일로 API Key를 발급하며 키 원문은 이 응답에서만 반환합니다.",
      security = @SecurityRequirement(name = "LoginJwtAuth"))
  public @interface IssueKey {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "API Key 재발행",
      description = "기존 키를 즉시 만료하고 대체 키 원문을 한 번만 반환합니다.",
      security = @SecurityRequirement(name = "LoginJwtAuth"))
  public @interface ReissueKey {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "API Key 만료",
      description = "현재 로그인 사용자가 소유한 API Key를 만료합니다. 반복 요청은 같은 결과를 반환합니다.",
      security = @SecurityRequirement(name = "LoginJwtAuth"))
  public @interface ExpireKey {}
}
