package app.presentation;

import app.application.admin.AdminStatisticsService;
import app.application.admin.AdminStatisticsView;
import app.core.data.response.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 통계 API입니다. 접근 통제는 SecurityConfig의 /api/v1/admin/** 규칙이 담당합니다. */
@RestController
@RequiredArgsConstructor
@Hidden
@RequestMapping(value = "/api/v1/admin/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminStatisticsController {

  private final AdminStatisticsService adminStatisticsService;

  /**
   * 통계 개요를 조회합니다. 화면이 모든 지표를 한 번에 그리므로 응답 하나로 돌려줍니다.
   *
   * @param days 조회 기간. 7, 30, 90만 허용하며 값이 없으면 7일입니다.
   */
  @GetMapping
  public ResponseEntity<ApiResponse<AdminStatisticsView>> overview(
      @RequestParam(required = false) Integer days) {
    return noStore(adminStatisticsService.load(days));
  }

  private <T> ResponseEntity<ApiResponse<T>> noStore(T data) {
    ResponseEntity<ApiResponse<T>> response = ApiResponse.ok(data);
    return ResponseEntity.status(response.getStatusCode())
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(response.getBody());
  }
}
