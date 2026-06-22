package app.presentation;

import app.application.EmailService;
import app.application.client.ClientMetadataReader;
import app.application.client.ClientsCommandService;
import app.core.data.response.ApiResponse;
import app.core.data.response.Status;
import app.core.data.response.constant.StatusCode;
import app.domain.client.ClientMetadata;
import app.dto.request.ClientRegistCommand;
import app.dto.request.ClientRegistRequest;
import app.dto.request.ClientUpdateRequest;
import app.dto.request.MailPayloadRequest;
import app.dto.response.ApiKeyReissueResponse;
import app.dto.response.ClientsRegistResponse;
import app.dto.response.EmailVerificationResponse;
import app.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/clients", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Clients", description = "API Key 발급 및 클라이언트 정보 관리 API")
public class ClientsController {
  private final ClientsCommandService clientsCommandService;
  private final ClientMetadataReader clientReader;
  private final EmailService emailService;

  @Operation(
      summary = "클라이언트 정보 확인",
      description = "발급된 API Key를 사용하여 가입 시 작성한 클라이언트 정보를 확인합니다.",
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  @GetMapping
  public ResponseEntity<ApiResponse<ClientMetadata>> get() {
    final String apikey = SecurityContextUtil.getCurrentApikey();
    if (apikey == null || apikey.isBlank()) {
      return ApiResponse.error(Status.of(StatusCode.UNAUTHORIZED));
    }
    ClientMetadata read = clientReader.read(apikey);
    return ApiResponse.ok(read);
  }

  @Operation(
      summary = "클라이언트 폐기",
      description = "발급된 API Key를 사용하여 클라이언트 정보를 폐기합니다.",
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  @DeleteMapping
  public ResponseEntity<ApiResponse<Boolean>> discardClient() {
    final String apikey = SecurityContextUtil.getCurrentApikey();
    clientsCommandService.discardClient(apikey);
    return ApiResponse.ok(Boolean.TRUE);
  }

  @Operation(
      summary = "신규 클라이언트 등록",
      description =
          """
          사용자 정보를 등록하고 API Key를 발급합니다.
          생성 시 입력 정보는 최대한 실제 정보를 입력해야 하며, 비정상적인 발급 요청은 무통보 제거될 수 있습니다.
          발급된 API Key는 반드시 안전하게 보관해야 합니다.
          """)
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<ClientsRegistResponse>> registerClient(
      @RequestBody @Valid ClientRegistRequest request) {
    final ClientRegistCommand command = request.toCommand();
    ClientsRegistResponse response = clientsCommandService.registerNewClient(command);
    return ApiResponse.ok(response);
  }

  @Operation(
      summary = "클라이언트 정보 업데이트",
      description = "발급된 API Key를 사용하여 클라이언트 발급자 정보와 메모를 업데이트합니다.",
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  @PostMapping("/update")
  public ResponseEntity<ApiResponse<ClientMetadata>> updateClient(
      @RequestBody @Valid ClientUpdateRequest request) {
    final String apikey = SecurityContextUtil.getCurrentApikey();
    final String issuerInfo = request.issuerInfo();
    final String note = request.note();

    ClientMetadata response = clientsCommandService.updateClientInfo(apikey, issuerInfo, note);
    return ApiResponse.ok(response);
  }

  @Operation(
      summary = "API Key 재발급",
      description = "발급된 API Key를 사용하여 새 API Key를 재발급합니다. 추후 이메일 인증 등 보안 강화 처리가 추가될 수 있습니다.",
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  @PostMapping("/reissue")
  public ResponseEntity<ApiResponse<ApiKeyReissueResponse>> regenerateApiKey() {
    String currentApiKey = SecurityContextUtil.getCurrentApikey();
    String newApiKey = clientsCommandService.regenerateApiKey(currentApiKey);
    return ApiResponse.ok(new ApiKeyReissueResponse(newApiKey));
  }

  @Operation(summary = "이메일 인증 코드 발송", description = "발급한 이메일을 통해 인증 코드를 전송합니다.")
  @GetMapping("/send-email")
  public ResponseEntity<ApiResponse<String>> sendEmail(
      @Parameter(description = "인증 코드를 받을 이메일", required = true) @RequestParam("email")
          String email) {
    boolean verified = clientReader.verifyClientByEmail(email);
    if (Boolean.FALSE.equals(verified)) {
      return ApiResponse.error(Status.of(StatusCode.BAD_REQUEST, "해당 이메일로 가입된 사용자가 없습니다."));
    }
    emailService.sendEmailNotice(email);
    return ApiResponse.ok("send email");
  }

  @Operation(summary = "이메일 인증 코드 검증", description = "이메일과 인증 코드를 확인하고 인증된 API Key를 반환합니다.")
  @PutMapping("/send-email")
  public ResponseEntity<ApiResponse<EmailVerificationResponse>> verifyEmail(
      @Valid @RequestBody MailPayloadRequest request) {
    boolean verified = emailService.verifyEmailCode(request.email(), request.code());
    if (Boolean.TRUE.equals(verified)) {
      String apikey = clientReader.getApiKeyByEmail(request.email());
      return ApiResponse.ok(new EmailVerificationResponse(apikey));
    } else {
      return ApiResponse.error(Status.of(StatusCode.BAD_REQUEST, "이메일 인증 코드가 올바르지 않습니다."));
    }
  }
}
