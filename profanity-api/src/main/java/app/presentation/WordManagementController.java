package app.presentation;

import app.application.manage.WordManagementService;
import app.core.data.response.ApiResponse;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.client.PermissionsType;
import app.dto.request.WordRequest;
import app.dto.response.MessageResponse;
import app.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@RequestMapping("/api/v1/word")
@RestController
@Tag(name = "Word Management", description = "비속어 단어 추가, 삭제, 수정 요청 API")
public class WordManagementController {
  private final WordManagementService wordManagement;

  @Operation(
      summary = "단어 변경 요청",
      description =
          """
          비속어 단어의 추가, 삭제, 수정을 요청합니다.
          severity는 LOW, MEDIUM, HIGH를 사용하고 type은 ADD, REMOVE, MODIFY를 사용합니다.
          """,
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  @PostMapping(value = "/request", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<MessageResponse>> requestNewWord(
      @RequestBody @Valid WordRequest request) {
    // 신규 비속어 등록 요청 로직
    log.info("request: {}", request);
    String word = request.word();
    String reason = request.reason();
    String severity = request.severity().name();
    UUID currentUserId = SecurityContextUtil.getCurrentUserId();
    var response =
        switch (request.type()) {
          case ADD -> wordManagement.requestNewWord(currentUserId, word, reason, severity);
          case REMOVE -> wordManagement.exceptionWord(currentUserId, word, reason, severity);
          case MODIFY -> wordManagement.modifyWord(currentUserId, word, reason, severity);
        };
    return ApiResponse.ok(response);
  }

  @Operation(
      summary = "단어 변경 요청 승인",
      description = "WRITE 권한을 가진 클라이언트가 단어 변경 요청을 승인합니다.",
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  @PostMapping("/accept/{requestId}")
  public ResponseEntity<?> acceptWord(
      @Parameter(description = "승인할 요청 ID 목록", required = true) @PathVariable
          List<Long> requestId) {
    final String write = PermissionsType.WRITE.getValue();
    List<String> currentUserPermissions = SecurityContextUtil.getCurrentUserPermissions();
    log.info("currentUserPermissions: {}", currentUserPermissions);
    if (Boolean.FALSE.equals(currentUserPermissions.contains(write))) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "권한이 없습니다.");
    }
    return ApiResponse.ok(wordManagement.acceptWord(requestId));
  }
}
