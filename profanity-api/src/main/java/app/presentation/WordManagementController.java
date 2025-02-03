package app.presentation;

import app.core.data.response.ApiResponse;
import app.dto.request.WordRequest;
import app.security.SecurityContextUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    // 신규 비속어 등록 요청
    @PostMapping("/request")
    public ApiResponse<?> requestNewWord(@RequestBody WordRequest request) {
        // 신규 비속어 등록 요청 로직
        return null;
    }

    // 비속어 등록 승인 (AI 검증 혹은 관리자 승인)
    @PostMapping("/accept/{requestId}")
    public ApiResponse<?> acceptWord(@PathVariable Long requestId) {
        return null;
        // 비속어 승인 및 등록 로직
    }

    // 비속어 제외 요청
    @PostMapping("/remove")
    public ApiResponse<?> requestRemoveWord(@RequestBody WordRequest request) {
        return null;
        // 비속어 제외 요청 로직
    }

    // 요청 상태 조회
    @GetMapping("/request/{requestId}")
    public ApiResponse<?> getRequestStatus(@PathVariable Long requestId) {
        return null;
        // 요청 상태 조회 로직
    }

    private void getPermission(){
        List<String> currentUserPermissions = SecurityContextUtil.getCurrentUserPermissions();
    }

}
