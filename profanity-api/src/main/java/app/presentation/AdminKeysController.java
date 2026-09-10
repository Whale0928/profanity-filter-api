package app.presentation;

import app.application.admin.AdminApiKeyService;
import app.application.admin.AdminApiKeyService.AdminApiKeyView;
import app.application.admin.AdminApiKeyService.ApiKeyStatus;
import app.core.data.response.ApiResponse;
import app.core.data.response.PageView;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import app.dto.request.admin.RevokeApiKeyRequest;
import app.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 API Key 조회와 폐기 API입니다. 응답에 키 원문과 해시를 포함하지 않습니다. */
@RestController
@RequiredArgsConstructor
@Hidden
@RequestMapping(value = "/api/v1/admin/keys", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminKeysController {

  private final AdminApiKeyService adminApiKeyService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageView<AdminApiKeyView>>> list(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) ApiKeyStatus status,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    PageResult<AdminApiKeyView> result =
        adminApiKeyService.search(query, status, PageQuery.of(page, size));
    return noStore(new PageView<>(result.items(), result.page(), result.hasNext()));
  }

  @PostMapping(value = "/{apiKeyId}/revoke", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<AdminApiKeyView>> revoke(
      @PathVariable UUID apiKeyId, @Valid @RequestBody RevokeApiKeyRequest request) {
    return noStore(
        adminApiKeyService.revoke(
            SecurityContextUtil.getCurrentLoginUserId(), apiKeyId, request.reason()));
  }

  private <T> ResponseEntity<ApiResponse<T>> noStore(T data) {
    ResponseEntity<ApiResponse<T>> response = ApiResponse.ok(data);
    return ResponseEntity.status(response.getStatusCode())
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(response.getBody());
  }
}
