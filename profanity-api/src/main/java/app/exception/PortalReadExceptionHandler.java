package app.exception;

import app.core.data.response.ApiResponse;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.presentation.DashboardInquiryController;
import app.presentation.NewsController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 소식과 본인 문의 조회에서만 HTTP 상태 코드를 함께 사용합니다.
 *
 * <p>이 서비스의 다른 경로는 비즈니스 결과를 HTTP 200 응답의 status.code로 전달합니다. 다만 비공개 소식과 남의 문의는 브라우저와 외부 도구가 곧바로 구분할
 * 수 있어야 하므로, 응답 본문 형식은 그대로 두고 상태 코드만 404와 403으로 내려보냅니다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {NewsController.class, DashboardInquiryController.class})
public class PortalReadExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
    return ApiResponse.error(httpStatusOf(exception), exception.getStatus());
  }

  private static HttpStatus httpStatusOf(BusinessException exception) {
    Integer code = exception.getStatus().code();
    if (matches(code, StatusCode.NEWS_NOT_FOUND) || matches(code, StatusCode.INQUIRY_NOT_FOUND)) {
      return HttpStatus.NOT_FOUND;
    }
    if (matches(code, StatusCode.INQUIRY_ACCESS_DENIED)) {
      return HttpStatus.FORBIDDEN;
    }
    return HttpStatus.OK;
  }

  private static boolean matches(Integer code, StatusCode statusCode) {
    return code != null && code == statusCode.code();
  }
}
