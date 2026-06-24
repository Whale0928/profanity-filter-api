package app.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ClientsOpenApi {
  private ClientsOpenApi() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "Clients", description = "API Key 발급 및 클라이언트 정보 관리 API")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "클라이언트 정보 확인",
      description = "발급된 API Key를 사용하여 가입 시 작성한 클라이언트 정보를 확인합니다.",
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  public @interface GetClient {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "클라이언트 폐기",
      description = "발급된 API Key를 사용하여 클라이언트 정보를 폐기합니다.",
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  public @interface DiscardClient {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "신규 클라이언트 등록",
      description =
          """
          사용자 정보를 등록하고 API Key를 발급합니다.
          생성 시 입력 정보는 최대한 실제 정보를 입력해야 하며, 비정상적인 발급 요청은 무통보 제거될 수 있습니다.
          발급된 API Key는 반드시 안전하게 보관해야 합니다.
          """)
  public @interface RegisterClient {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "클라이언트 정보 업데이트",
      description = "발급된 API Key를 사용하여 클라이언트 발급자 정보와 메모를 업데이트합니다.",
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  public @interface UpdateClient {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "API Key 재발급",
      description = "발급된 API Key를 사용하여 새 API Key를 재발급합니다. 추후 이메일 인증 등 보안 강화 처리가 추가될 수 있습니다.",
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  public @interface RegenerateApiKey {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "이메일 인증 코드 발송",
      description = "발급한 이메일을 통해 인증 코드를 전송합니다.",
      parameters = @Parameter(name = "email", description = "인증 코드를 받을 이메일", required = true))
  public @interface SendEmail {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(summary = "이메일 인증 코드 검증", description = "이메일과 인증 코드를 확인하고 인증된 API Key를 반환합니다.")
  public @interface VerifyEmail {}
}
