package app.presentation;

import app.application.admin.AdminUserService;
import app.application.admin.AdminUserService.AdminUserView;
import app.core.data.response.ApiResponse;
import app.core.data.response.PageView;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import app.domain.user.UserRole;
import app.dto.request.admin.ChangeUserStatusRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 사용자 관리 API입니다. 역할 변경 엔드포인트는 이번 범위에서 제공하지 않습니다. */
@RestController
@RequiredArgsConstructor
@Hidden
@RequestMapping(value = "/api/v1/admin/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminUsersController {

  private final AdminUserService adminUserService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageView<AdminUserView>>> list(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) UserRole role,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    PageResult<AdminUserView> result =
        adminUserService.search(query, role, PageQuery.of(page, size));
    return noStore(new PageView<>(result.items(), result.page(), result.hasNext()));
  }

  @PatchMapping(value = "/{userId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<AdminUserView>> changeStatus(
      @PathVariable UUID userId, @Valid @RequestBody ChangeUserStatusRequest request) {
    return noStore(
        adminUserService.changeStatus(
            SecurityContextUtil.getCurrentLoginUserId(), userId, request.status()));
  }

  private <T> ResponseEntity<ApiResponse<T>> noStore(T data) {
    ResponseEntity<ApiResponse<T>> response = ApiResponse.ok(data);
    return ResponseEntity.status(response.getStatusCode())
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(response.getBody());
  }
}
