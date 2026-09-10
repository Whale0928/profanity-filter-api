package app.presentation;

import app.application.news.AdminNewsService;
import app.application.news.NewsView;
import app.application.news.NewsWriteCommand;
import app.core.data.response.ApiResponse;
import app.core.data.response.PageView;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import app.dto.request.admin.NewsWriteRequest;
import app.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 소식 관리 API입니다. 접근 통제는 SecurityConfig의 /api/v1/admin/** 규칙이 담당합니다. */
@RestController
@RequiredArgsConstructor
@Hidden
@RequestMapping(value = "/api/v1/admin/news", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminNewsController {

  private final AdminNewsService adminNewsService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageView<NewsView>>> list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String query,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    PageResult<NewsView> result =
        adminNewsService.search(status, category, query, PageQuery.of(page, size));
    return noStore(new PageView<>(result.items(), result.page(), result.hasNext()));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<NewsView>> create(
      @Valid @RequestBody NewsWriteRequest request) {
    return noStore(
        adminNewsService.create(SecurityContextUtil.getCurrentLoginUserId(), command(request)));
  }

  @PutMapping(value = "/{newsId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<NewsView>> update(
      @PathVariable Long newsId, @Valid @RequestBody NewsWriteRequest request) {
    return noStore(
        adminNewsService.update(
            SecurityContextUtil.getCurrentLoginUserId(), newsId, command(request)));
  }

  @DeleteMapping("/{newsId}")
  public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable Long newsId) {
    adminNewsService.delete(SecurityContextUtil.getCurrentLoginUserId(), newsId);
    return noStore(true);
  }

  private static NewsWriteCommand command(NewsWriteRequest request) {
    return NewsWriteCommand.of(
        request.title(), request.category(), request.content(), request.status());
  }

  private <T> ResponseEntity<ApiResponse<T>> noStore(T data) {
    ResponseEntity<ApiResponse<T>> response = ApiResponse.ok(data);
    return ResponseEntity.status(response.getStatusCode())
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(response.getBody());
  }
}
