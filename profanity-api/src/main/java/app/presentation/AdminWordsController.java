package app.presentation;

import app.application.admin.AdminWordService;
import app.application.admin.AdminWordService.AdminWordView;
import app.core.data.response.ApiResponse;
import app.core.data.response.PageView;
import app.domain.profanity.constant.isUsedType;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import app.dto.request.admin.CreateWordRequest;
import app.dto.request.admin.UpdateWordRequest;
import app.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 필터 사전 관리 API입니다. 접근 통제는 SecurityConfig의 /api/v1/admin/** 규칙이 담당합니다. */
@RestController
@RequiredArgsConstructor
@Hidden
@RequestMapping(value = "/api/v1/admin/words", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminWordsController {

  private final AdminWordService adminWordService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageView<AdminWordView>>> list(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) isUsedType isUsed,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    PageResult<AdminWordView> result =
        adminWordService.search(query, isUsed, PageQuery.of(page, size));
    return noStore(new PageView<>(result.items(), result.page(), result.hasNext()));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<AdminWordView>> create(
      @Valid @RequestBody CreateWordRequest request) {
    return noStore(
        adminWordService.create(SecurityContextUtil.getCurrentLoginUserId(), request.word()));
  }

  @PutMapping(value = "/{wordId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<AdminWordView>> update(
      @PathVariable Long wordId, @Valid @RequestBody UpdateWordRequest request) {
    return noStore(
        adminWordService.update(
            SecurityContextUtil.getCurrentLoginUserId(), wordId, request.word(), request.isUsed()));
  }

  private <T> ResponseEntity<ApiResponse<T>> noStore(T data) {
    ResponseEntity<ApiResponse<T>> response = ApiResponse.ok(data);
    return ResponseEntity.status(response.getStatusCode())
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(response.getBody());
  }
}
