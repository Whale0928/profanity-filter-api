package app.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class AuthOpenApi {
  private AuthOpenApi() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "Authentication", description = "SSO 로그인 JWT와 refresh token 관리 API")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(summary = "로그인 코드 교환", description = "SSO callback에서 발급된 일회용 코드를 로그인 token으로 교환합니다.")
  public @interface Exchange {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(summary = "CSRF token 조회", description = "refresh 요청에 필요한 CSRF token을 반환합니다.")
  public @interface Csrf {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "로그인 token 갱신",
      description = "HttpOnly refresh cookie를 rotate하고 새 access token을 반환합니다.")
  public @interface Refresh {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "로그인 사용자 조회",
      description = "LOGIN_JWT로 인증된 현재 사용자를 반환합니다.",
      security = @SecurityRequirement(name = "LoginJwtAuth"))
  public @interface Me {}
}
