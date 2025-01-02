package app.presentation;

import app.application.apikey.ClientsCommandService;
import app.application.client.ClientMetadataReader;
import app.core.data.response.ApiResponse;
import app.domain.client.ClientMetadata;
import app.dto.request.ClientRegistCommand;
import app.dto.request.ClientRegistRequest;
import app.dto.response.ClientsRegistResponse;
import app.security.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/clients")
public class ClientsController {
    private final ClientsCommandService clientsCommandService;
    private final ClientMetadataReader clientReader;

    @GetMapping
    public ResponseEntity<?> get() {
        final String apikey = SecurityContextUtil.getCurrentApikey();
        ClientMetadata read = clientReader.read(apikey);
        return ApiResponse.ok(read);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerClient(
            @RequestBody @Valid ClientRegistRequest request
    ) {
        final ClientRegistCommand command = request.toCommand();
        // todo: 이메일 인증 구현
        ClientsRegistResponse response = clientsCommandService.registerNewClient(command);
        return ApiResponse.ok(response);
    }
}
