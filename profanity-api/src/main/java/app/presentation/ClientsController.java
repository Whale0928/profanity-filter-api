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
import app.dto.response.ClientsRegistResponse;
import app.security.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/clients")
public class ClientsController {
    private final ClientsCommandService clientsCommandService;
    private final ClientMetadataReader clientReader;
    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<?> get() {
        final String apikey = SecurityContextUtil.getCurrentApikey();
        if (apikey == null || apikey.isBlank()) {
            return ApiResponse.error(Status.of(StatusCode.UNAUTHORIZED));
        }
        ClientMetadata read = clientReader.read(apikey);
        return ApiResponse.ok(read);
    }

    @DeleteMapping
    public ResponseEntity<?> discardClient() {
        final String apikey = SecurityContextUtil.getCurrentApikey();
        clientsCommandService.discardClient(apikey);
        return ApiResponse.ok(Boolean.TRUE);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerClient(
            @RequestBody @Valid ClientRegistRequest request
    ) {
        final ClientRegistCommand command = request.toCommand();
        ClientsRegistResponse response = clientsCommandService.registerNewClient(command);
        return ApiResponse.ok(response);
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateClient(
            @RequestBody @Valid ClientUpdateRequest request
    ) {
        final String apikey = SecurityContextUtil.getCurrentApikey();
        final String issuerInfo = request.issuerInfo();
        final String note = request.note();

        ClientMetadata response = clientsCommandService.updateClientInfo(apikey, issuerInfo, note);
        return ApiResponse.ok(response);
    }

    @GetMapping("/send-email")
    public ResponseEntity<?> sendEmail(@RequestParam("email") String email) {
        boolean verified = clientReader.verifyClientByEmail(email);
        if (Boolean.FALSE.equals(verified)) {
            return ApiResponse.error(Status.of(StatusCode.BAD_REQUEST, "해당 이메일로 가입된 사용자가 없습니다."));
        }
        emailService.sendEmailNotice(email);
        return ApiResponse.ok("send email");
    }

    @PutMapping("/send-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody MailPayloadRequest request) {
        boolean verified = emailService.verifyEmailCode(request.email(), request.code());
        if (Boolean.TRUE.equals(verified)) {
            String apikey = clientReader.getApiKeyByEmail(request.email());
            Map<String, String> response = Map.of("apikey", apikey);
            return ApiResponse.ok(response);
        } else {
            return ApiResponse.error(Status.of(StatusCode.BAD_REQUEST, "이메일 인증 코드가 올바르지 않습니다."));
        }
    }

}
