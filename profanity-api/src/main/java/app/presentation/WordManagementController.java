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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@AllArgsConstructor
@RequestMapping("/api/v1/word")
@RestController
public class WordManagementController {
    private final WordManagementService wordManagement;

    // 신규 비속어 등록 요청
    @PostMapping("/request")
    public ResponseEntity<?> requestNewWord(@RequestBody @Valid WordRequest request) {
        // 신규 비속어 등록 요청 로직
        log.info("request: {}", request);
        String word = request.word();
        String reason = request.reason();
        String severity = request.severity().name();

        var response = switch (request.type()) {
            case ADD -> wordManagement.requestNewWord(word, reason, severity);
            case REMOVE -> wordManagement.exceptionWord(word, reason, severity);
            case MODIFY -> wordManagement.modifyWord(word, reason, severity);
        };
        return ApiResponse.ok(response);
    }

    // 비속어 등록 승인 (AI 검증 혹은 관리자 승인)
    @PostMapping("/accept/{requestId}")
    public ResponseEntity<?> acceptWord(@PathVariable List<Long> requestId) {
        checkWritePermission();
        return null;
        // 비속어 승인 및 등록 로직
    }

    // 비속어 제외 요청
    @PostMapping("/remove")
    public ResponseEntity<?> requestRemoveWord(@RequestBody WordRequest request) {
        return null;
        // 비속어 제외 요청 로직
    }

    // 요청 상태 조회
    @GetMapping("/request/{requestId}")
    public ApiResponse<?> getRequestStatus(@PathVariable Long requestId) {
        checkWritePermission();
        return null;
        // 요청 상태 조회 로직
    }

    private void checkWritePermission() {
        final String write = PermissionsType.WRITE.getValue();
        List<String> currentUserPermissions = SecurityContextUtil.getCurrentUserPermissions();
        log.info("currentUserPermissions: {}", currentUserPermissions);
        if (Boolean.FALSE.equals(currentUserPermissions.contains(write))) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "권한이 없습니다.");
        }
    }
}
