package app.presentation;

import app.application.manage.WordManagementService;
import app.core.data.response.ApiResponse;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.client.PermissionsType;
import app.dto.request.WordRequest;
import app.security.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@RequestMapping("/api/v1/word")
@RestController
public class WordManagementController {
    private final WordManagementService wordManagement;

    @PostMapping("/request")
    public ResponseEntity<?> requestNewWord(@RequestBody @Valid WordRequest request) {
        // 신규 비속어 등록 요청 로직
        log.info("request: {}", request);
        String word = request.word();
        String reason = request.reason();
        String severity = request.severity().name();
        UUID currentUserId = SecurityContextUtil.getCurrentUserId();
        var response = switch (request.type()) {
            case ADD -> wordManagement.requestNewWord(currentUserId, word, reason, severity);
            case REMOVE -> wordManagement.exceptionWord(currentUserId, word, reason, severity);
            case MODIFY -> wordManagement.modifyWord(currentUserId, word, reason, severity);
        };
        return ApiResponse.ok(response);
    }

    @PostMapping("/accept/{requestId}")
    public ResponseEntity<?> acceptWord(@PathVariable List<Long> requestId) {
        final String write = PermissionsType.WRITE.getValue();
        List<String> currentUserPermissions = SecurityContextUtil.getCurrentUserPermissions();
        log.info("currentUserPermissions: {}", currentUserPermissions);
        if (Boolean.FALSE.equals(currentUserPermissions.contains(write))) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "권한이 없습니다.");
        }
        return ApiResponse.ok(wordManagement.acceptWord(requestId));
    }
}
