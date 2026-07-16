package app.openapi;

import app.core.data.response.Status;
import app.dto.request.AuthCodeExchangeRequest;
import app.dto.response.CsrfTokenResponse;
import app.dto.response.LoginTokenResponse;
import app.dto.response.LoginUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public final class AuthOpenApi {
  private static final String TOKEN_RESPONSE_EXAMPLE =
      """
      {
        "status": {
          "code": 2000,
          "message": "Ok",
          "description": "정상적으로 처리 되었습니다.",
          "DetailDescription": ""
        },
        "data": {
          "accessToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6ImxvZ2luLWtleS0xIn0.example.signature",
          "tokenType": "Bearer",
          "expiresIn": 900,
          "user": {
            "id": "018f4fd8-9f6f-7d1a-9b80-3f1f8dd7c001",
            "displayName": "홍길동",
            "email": "user@example.com",
            "avatarUrl": "https://example.com/avatar.png"
          }
        }
      }
      """;

  private static final String USER_RESPONSE_EXAMPLE =
      """
      {
        "status": {
          "code": 2000,
          "message": "Ok",
          "description": "정상적으로 처리 되었습니다.",
          "DetailDescription": ""
        },
        "data": {
          "id": "018f4fd8-9f6f-7d1a-9b80-3f1f8dd7c001",
          "displayName": "홍길동",
          "email": "user@example.com",
          "avatarUrl": "https://example.com/avatar.png"
        }
      }
      """;

  private static final String INVALID_LOGIN_CODE_EXAMPLE =
      """
      {
        "status": {
          "code": 4012,
          "message": "Login_code_invalid",
          "description": "로그인 교환 코드가 유효하지 않습니다.",
          "DetailDescription": ""
        },
        "data": null
      }
      """;

  private static final String INVALID_EXCHANGE_REQUEST_EXAMPLE =
      """
      {
        "status": {
          "code": 4000,
          "message": "Bad_request",
          "description": "처리에 실패하였습니다. 요청이 잘못 되었거나 필수 파라미터가 누락된 경우 발생 합니다. Description에서 보다 상세한 오류 메세지를 확인할 수 있습니다.",
          "DetailDescription": "code: must not be blank  / "
        },
        "data": null
      }
      """;

  private static final String INVALID_REFRESH_TOKEN_EXAMPLE =
      """
      {
        "status": {
          "code": 4015,
          "message": "Refresh_token_invalid",
          "description": "로그인 refresh token이 유효하지 않습니다.",
          "DetailDescription": ""
        },
        "data": null
      }
      """;

  private static final String REUSED_REFRESH_TOKEN_EXAMPLE =
      """
      {
        "status": {
          "code": 4016,
          "message": "Refresh_token_reused",
          "description": "이미 사용한 refresh token이 다시 제출되었습니다.",
          "DetailDescription": ""
        },
        "data": null
      }
      """;

  private static final String INVALID_LOGIN_TOKEN_EXAMPLE =
      """
      {
        "status": {
          "code": 4013,
          "message": "Login_token_invalid",
          "description": "로그인 access token이 유효하지 않습니다.",
          "DetailDescription": ""
        },
        "data": null
      }
      """;

  private static final String FORBIDDEN_EXAMPLE =
      """
      {
        "status": {
          "code": 4030,
          "message": "Forbidden",
          "description": "인증 권한이 부적절합니다. 인증 키가 유효하지 않거나 권한이 없는 경우 발생합니다.",
          "DetailDescription": ""
        },
        "data": null
      }
      """;

  private static final String INACTIVE_USER_EXAMPLE =
      """
      {
        "status": {
          "code": 4033,
          "message": "User_inactive",
          "description": "비활성 사용자 계정입니다.",
          "DetailDescription": ""
        },
        "data": null
      }
      """;

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "Authentication", description = "SSO 로그인 JWT와 refresh token 관리 API")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "로그인 코드 교환",
      description =
          """
          OAuth2 로그인 성공 후 frontend redirect URI로 전달된 일회용 code를 로그인 token으로 교환합니다.
          code는 한 번만 사용할 수 있으며, 성공하면 access token은 응답 body로, refresh token은 HttpOnly cookie로 발급합니다.
          """,
      requestBody =
          @RequestBody(
              required = true,
              description = "OAuth2 로그인 완료 후 발급받은 일회용 교환 코드",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = AuthCodeExchangeRequest.class),
                      examples =
                          @ExampleObject(
                              name = "exchangeCode",
                              summary = "일회용 로그인 코드 교환",
                              value = "{\"code\":\"sso_exchange_code_example\"}"))),
      responses = {
        @ApiResponse(
            responseCode = "200",
            description =
                "access token 발급 및 refresh cookie 설정. 요청 검증 실패는 status.code 4000으로 반환됩니다.",
            headers =
                @Header(
                    name = HttpHeaders.SET_COOKIE,
                    description = "HttpOnly refresh token cookie",
                    schema = @Schema(type = "string")),
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = LoginTokenApiResponse.class),
                    examples = {
                      @ExampleObject(name = "success", value = TOKEN_RESPONSE_EXAMPLE),
                      @ExampleObject(
                          name = "invalidRequest",
                          summary = "code 누락 또는 공백",
                          value = INVALID_EXCHANGE_REQUEST_EXAMPLE)
                    })),
        @ApiResponse(
            responseCode = "401",
            description = "교환 코드가 유효하지 않거나 만료 또는 이미 소비됨",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorApiResponse.class),
                    examples =
                        @ExampleObject(name = "invalidCode", value = INVALID_LOGIN_CODE_EXAMPLE))),
        @ApiResponse(
            responseCode = "403",
            description = "연결된 사용자 계정이 비활성 상태임",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorApiResponse.class),
                    examples =
                        @ExampleObject(name = "inactiveUser", value = INACTIVE_USER_EXAMPLE)))
      })
  public @interface Exchange {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "CSRF token 조회",
      description =
          """
          refresh 요청에 사용할 CSRF token을 발급합니다.
          응답의 headerName과 token을 refresh 요청 header에 넣고, 함께 발급된 XSRF-TOKEN cookie도 전송해야 합니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "CSRF header 이름과 token 반환 및 XSRF-TOKEN cookie 설정",
              headers =
                  @Header(
                      name = HttpHeaders.SET_COOKIE,
                      description = "CSRF 검증용 XSRF-TOKEN cookie",
                      schema = @Schema(type = "string")),
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = CsrfTokenApiResponse.class),
                      examples =
                          @ExampleObject(
                              name = "csrfToken",
                              value =
                                  """
                                  {
                                    "status": {
                                      "code": 2000,
                                      "message": "Ok",
                                      "description": "정상적으로 처리 되었습니다.",
                                      "DetailDescription": ""
                                    },
                                    "data": {
                                      "headerName": "X-XSRF-TOKEN",
                                      "token": "csrf_token_example"
                                    }
                                  }
                                  """))))
  public @interface Csrf {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "로그인 token 갱신",
      description =
          """
          refresh token을 rotate하고 새 access token과 refresh token을 발급합니다.
          먼저 GET /api/v1/auth/csrf를 호출한 뒤 PF_LOGIN_REFRESH 및 XSRF-TOKEN cookie와 X-XSRF-TOKEN header를 함께 전송해야 합니다.
          이미 사용한 refresh token을 재사용하면 보안 정책에 따라 요청 또는 token family가 거부됩니다.
          """,
      parameters = {
        @Parameter(
            name = "PF_LOGIN_REFRESH",
            in = ParameterIn.COOKIE,
            required = true,
            description = "HttpOnly refresh token cookie. 브라우저가 자동으로 전송합니다.",
            schema = @Schema(type = "string")),
        @Parameter(
            name = "XSRF-TOKEN",
            in = ParameterIn.COOKIE,
            required = true,
            description = "GET /api/v1/auth/csrf에서 발급된 CSRF cookie",
            schema = @Schema(type = "string")),
        @Parameter(
            name = "X-XSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "GET /api/v1/auth/csrf 응답의 data.token 값",
            schema = @Schema(type = "string"))
      },
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "새 access token 발급 및 refresh cookie 교체",
            headers =
                @Header(
                    name = HttpHeaders.SET_COOKIE,
                    description = "rotate된 HttpOnly refresh token cookie",
                    schema = @Schema(type = "string")),
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = LoginTokenApiResponse.class),
                    examples = @ExampleObject(name = "success", value = TOKEN_RESPONSE_EXAMPLE))),
        @ApiResponse(
            responseCode = "401",
            description = "refresh token이 무효·만료되었거나 이미 사용됨",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorApiResponse.class),
                    examples = {
                      @ExampleObject(name = "invalidToken", value = INVALID_REFRESH_TOKEN_EXAMPLE),
                      @ExampleObject(name = "reusedToken", value = REUSED_REFRESH_TOKEN_EXAMPLE)
                    })),
        @ApiResponse(
            responseCode = "403",
            description = "CSRF 검증 실패 또는 사용자 계정 비활성",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorApiResponse.class),
                    examples = {
                      @ExampleObject(name = "csrfRejected", value = FORBIDDEN_EXAMPLE),
                      @ExampleObject(name = "inactiveUser", value = INACTIVE_USER_EXAMPLE)
                    }))
      })
  public @interface Refresh {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "로그인 사용자 조회",
      description = "Authorization: Bearer <access-token>으로 인증된 현재 로그인 사용자를 반환합니다.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "현재 로그인 사용자 정보",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = LoginUserApiResponse.class),
                    examples =
                        @ExampleObject(name = "currentUser", value = USER_RESPONSE_EXAMPLE))),
        @ApiResponse(
            responseCode = "401",
            description = "access token이 누락·무효·만료됨",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorApiResponse.class),
                    examples =
                        @ExampleObject(
                            name = "invalidToken",
                            value = INVALID_LOGIN_TOKEN_EXAMPLE))),
        @ApiResponse(
            responseCode = "403",
            description = "사용자 계정이 비활성 상태임",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorApiResponse.class),
                    examples =
                        @ExampleObject(name = "inactiveUser", value = INACTIVE_USER_EXAMPLE)))
      },
      security = @SecurityRequirement(name = "LoginJwtAuth"))
  public @interface Me {}

  @Schema(name = "LoginTokenApiResponse", description = "로그인 token 발급 응답")
  public record LoginTokenApiResponse(
      Status status, LoginTokenResponse data, Map<String, Object> meta) {}

  @Schema(name = "CsrfTokenApiResponse", description = "CSRF token 발급 응답")
  public record CsrfTokenApiResponse(
      Status status, CsrfTokenResponse data, Map<String, Object> meta) {}

  @Schema(name = "LoginUserApiResponse", description = "로그인 사용자 조회 응답")
  public record LoginUserApiResponse(
      Status status, LoginUserResponse data, Map<String, Object> meta) {}

  @Schema(name = "AuthErrorApiResponse", description = "로그인 인증 오류 응답")
  public record ErrorApiResponse(Status status, Object data, Map<String, Object> meta) {}
}
