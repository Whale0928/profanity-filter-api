package app.presentation;

import app.application.whitelist.WhitelistService;
import app.application.whitelist.WhitelistService.WhitelistView;
import app.core.data.response.ApiResponse;
import app.dto.request.WhitelistWriteRequest;
import app.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 사용자의 허용 단어 그룹 관리 API입니다. 접근 통제는 SecurityConfig의 /api/v1/dashboard/** 규칙이 담당합니다. */
@RestController
@RequiredArgsConstructor
@Hidden
@RequestMapping(value = "/api/v1/dashboard/whitelists", produces = MediaType.APPLICATION_JSON_VALUE)
public class DashboardWhitelistController {

  private final WhitelistService whitelistService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<WhitelistView>>> list() {
    return noStore(whitelistService.findAll(SecurityContextUtil.getCurrentLoginUserId()));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<WhitelistView>> create(
      @Valid @RequestBody WhitelistWriteRequest request) {
    return noStore(
        whitelistService.create(
            SecurityContextUtil.getCurrentLoginUserId(), request.name(), request.words()));
  }

  @PutMapping(value = "/{whitelistId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<WhitelistView>> update(
      @PathVariable UUID whitelistId, @Valid @RequestBody WhitelistWriteRequest request) {
    return noStore(
        whitelistService.update(
            SecurityContextUtil.getCurrentLoginUserId(),
            whitelistId,
            request.name(),
            request.words()));
  }

  @DeleteMapping("/{whitelistId}")
  public ResponseEntity<ApiResponse<Map<String, UUID>>> delete(@PathVariable UUID whitelistId) {
    whitelistService.delete(SecurityContextUtil.getCurrentLoginUserId(), whitelistId);
    return noStore(Map.of("id", whitelistId));
  }

  private <T> ResponseEntity<ApiResponse<T>> noStore(T data) {
    ResponseEntity<ApiResponse<T>> response = ApiResponse.ok(data);
    return ResponseEntity.status(response.getStatusCode())
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(response.getBody());
  }
}
