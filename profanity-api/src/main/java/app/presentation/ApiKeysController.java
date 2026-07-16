package app.presentation;

import app.application.apikey.ApiKeyManagementService;
import app.application.apikey.ApiKeyManagementService.ApiKeyView;
import app.application.apikey.ApiKeyManagementService.IssuedApiKey;
import app.core.data.response.ApiResponse;
import app.dto.request.CreateApiKeyRequest;
import app.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Hidden
@RequestMapping(value = "/api/v1/dashboard/keys", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiKeysController {
  private final ApiKeyManagementService apiKeyManagementService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<ApiKeyView>>> list() {
    return noStore(apiKeyManagementService.findAll(SecurityContextUtil.getCurrentLoginUserId()));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<IssuedApiKey>> issue(
      @Valid @RequestBody CreateApiKeyRequest request) {
    return noStore(
        apiKeyManagementService.issue(
            SecurityContextUtil.getCurrentLoginUserId(),
            SecurityContextUtil.getCurrentUserEmail(),
            request.toCommand()));
  }

  @PostMapping("/{apiKeyId}/reissue")
  public ResponseEntity<ApiResponse<IssuedApiKey>> reissue(@PathVariable UUID apiKeyId) {
    return noStore(
        apiKeyManagementService.reissue(SecurityContextUtil.getCurrentLoginUserId(), apiKeyId));
  }

  @DeleteMapping("/{apiKeyId}")
  public ResponseEntity<ApiResponse<ApiKeyView>> expire(@PathVariable UUID apiKeyId) {
    return noStore(
        apiKeyManagementService.expire(SecurityContextUtil.getCurrentLoginUserId(), apiKeyId));
  }

  private <T> ResponseEntity<ApiResponse<T>> noStore(T data) {
    ResponseEntity<ApiResponse<T>> response = ApiResponse.ok(data);
    return ResponseEntity.status(response.getStatusCode())
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(response.getBody());
  }
}
