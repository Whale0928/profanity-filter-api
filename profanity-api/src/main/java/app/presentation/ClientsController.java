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
import app.openapi.ClientsOpenApi;
import app.security.SecurityContextUtil;
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
@ClientsOpenApi.ApiTag
public class ClientsController {
  private final ClientsCommandService clientsCommandService;
  private final ClientMetadataReader clientReader;
  private final EmailService emailService;

  @ClientsOpenApi.GetClient
  @GetMapping
  public ResponseEntity<ApiResponse<ClientMetadata>> get() {
    final String apikey = SecurityContextUtil.getCurrentApikey();
    if (apikey == null || apikey.isBlank()) {
      return ApiResponse.error(Status.of(StatusCode.UNAUTHORIZED));
    }
    ClientMetadata read = clientReader.read(apikey);
    return ApiResponse.ok(read);
  }

  @ClientsOpenApi.DiscardClient
  @DeleteMapping
  public ResponseEntity<ApiResponse<Boolean>> discardClient() {
    final String apikey = SecurityContextUtil.getCurrentApikey();
    clientsCommandService.discardClient(apikey);
    return ApiResponse.ok(Boolean.TRUE);
  }

  @ClientsOpenApi.RegisterClient
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<ClientsRegistResponse>> registerClient(
      @RequestBody @Valid ClientRegistRequest request) {
    final ClientRegistCommand command = request.toCommand();
    ClientsRegistResponse response = clientsCommandService.registerNewClient(command);
    return ApiResponse.ok(response);
  }

  @ClientsOpenApi.UpdateClient
  @PostMapping("/update")
  public ResponseEntity<ApiResponse<ClientMetadata>> updateClient(
      @RequestBody @Valid ClientUpdateRequest request) {
    final String apikey = SecurityContextUtil.getCurrentApikey();
    final String issuerInfo = request.issuerInfo();
    final String note = request.note();

    ClientMetadata response = clientsCommandService.updateClientInfo(apikey, issuerInfo, note);
    return ApiResponse.ok(response);
  }

  @ClientsOpenApi.RegenerateApiKey
  @PostMapping("/reissue")
  public ResponseEntity<ApiResponse<ApiKeyReissueResponse>> regenerateApiKey() {
    String currentApiKey = SecurityContextUtil.getCurrentApikey();
    String newApiKey = clientsCommandService.regenerateApiKey(currentApiKey);
    return ApiResponse.ok(new ApiKeyReissueResponse(newApiKey));
  }

  @ClientsOpenApi.SendEmail
  @GetMapping("/send-email")
  public ResponseEntity<ApiResponse<String>> sendEmail(@RequestParam("email") String email) {
    boolean verified = clientReader.verifyClientByEmail(email);
    if (Boolean.FALSE.equals(verified)) {
      return ApiResponse.error(Status.of(StatusCode.BAD_REQUEST, "해당 이메일로 가입된 사용자가 없습니다."));
    }
    emailService.sendEmailNotice(email);
    return ApiResponse.ok("send email");
  }

  @ClientsOpenApi.VerifyEmail
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
