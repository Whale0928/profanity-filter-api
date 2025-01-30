package app.presentation;

import app.application.EmailService;
import app.application.apikey.ClientsCommandService;
import app.application.client.ClientMetadataReader;
import app.core.data.response.ApiResponse;
import app.core.data.response.Status;
import app.core.data.response.constant.StatusCode;
import app.domain.client.ClientMetadata;
import app.dto.request.ClientRegistCommand;
import app.dto.request.ClientRegistRequest;
import app.dto.request.MailPayloadRequest;
import app.dto.response.ClientsRegistResponse;
import app.security.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/register")
    public ResponseEntity<?> registerClient(
            @RequestBody @Valid ClientRegistRequest request
    ) {
        final ClientRegistCommand command = request.toCommand();
        ClientsRegistResponse response = clientsCommandService.registerNewClient(command);
        return ApiResponse.ok(response);
    }

    @GetMapping("/send-email")
    public ResponseEntity<?> sendEmail(@RequestParam("email") String email) {
        //todo: 해당 사용자로 가입된 유저가 있는지 확인
        emailService.sendEmailNotice(email);
        return ApiResponse.ok("send email");
    }

    @PutMapping("/send-email")
    public ResponseEntity<?> validEmail(@Valid @RequestBody MailPayloadRequest request) {
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
