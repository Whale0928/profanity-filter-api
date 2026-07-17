package app.exception;

import app.application.HttpClient;
import app.core.data.response.ApiResponse;
import app.core.data.response.Status;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.security.authentication.CredentialAuthenticationException;
import app.security.login.LoginFlowException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
    log.warn("비즈니스 예외 발생: {}", ex.getMessage());
    return ApiResponse.error(ex.getStatus());
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
      BadCredentialsException ex, HttpServletRequest request) {
    StatusCode resolve = StatusCode.resolve(ex.getMessage());
    log.warn(
        "[AUTH] 인증 거절 statusCode={} httpStatus={} method={} path={} host={} clientIp={} credential={} cfRay={} exceptionType={}",
        resolve.code(),
        HttpStatus.OK.value(),
        safe(request.getMethod(), 16),
        safe(request.getRequestURI(), 256),
        safe(request.getServerName(), 255),
        safe(HttpClient.getClientIP(request), 64),
        credentialState(request),
        safe(request.getHeader("CF-Ray"), 64),
        ex.getClass().getSimpleName());
    return ApiResponse.error(Status.of(resolve));
  }

  private static String credentialState(HttpServletRequest request) {
    String apiKey = request.getHeader("X-API-KEY");
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (apiKey != null && authorization != null) {
      return "MULTIPLE";
    }
    if (apiKey != null) {
      return apiKey.isBlank() ? "API_KEY_BLANK" : "API_KEY_PRESENT";
    }
    if (authorization != null) {
      return authorization.isBlank() ? "BEARER_BLANK" : "BEARER_PRESENT";
    }
    return "NONE";
  }

  private static String safe(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return "none";
    }
    String sanitized = value.replaceAll("[\\r\\n\\t]", "_");
    return sanitized.substring(0, Math.min(sanitized.length(), maxLength));
  }

  @ExceptionHandler(LoginFlowException.class)
  public ResponseEntity<ApiResponse<Void>> handleLoginFlowException(LoginFlowException ex) {
    log.warn("로그인 인증 예외 발생: {}", ex.getStatusCode().name());
    return ApiResponse.error(ex.getHttpStatus(), Status.of(ex.getStatusCode()));
  }

  @ExceptionHandler(CredentialAuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleCredentialAuthenticationException(
      CredentialAuthenticationException ex) {
    StatusCode statusCode = StatusCode.resolve(ex.getMessage());
    log.warn("요청 인증 예외 발생: {}", statusCode.name());
    return ApiResponse.error(ex.httpStatus(), Status.of(statusCode));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
      AuthenticationException ex) {
    log.warn("인증 예외 발생: {}", ex.getMessage());
    StatusCode resolve = StatusCode.resolve(ex.getMessage());
    return ApiResponse.error(Status.of(resolve));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
    log.warn("접근 권한 예외 발생: {}", ex.getClass().getSimpleName());
    return ApiResponse.error(HttpStatus.FORBIDDEN, Status.of(StatusCode.FORBIDDEN));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    log.debug("유효성 검증 예외 발생: {}", ex.getMessage());
    return ApiResponse.error(
        Status.of(
            StatusCode.BAD_REQUEST,
            ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex) {
    log.warn("JSON 파싱 예외 발생: {}", ex.getMessage());
    String message = ex.getMessage();
    if (message != null) {
      // problem: 뒤의 실제 에러 메시지 추출
      int problemIndex = message.indexOf("problem:");
      if (problemIndex != -1) {
        message = message.substring(problemIndex + 9).trim();
      } else {
        int colonIndex = message.indexOf(':');
        if (colonIndex != -1) {
          message = "요청 데이터 형식이 올바르지 않습니다";
        }
      }
    }
    return ApiResponse.error(Status.of(StatusCode.BAD_REQUEST, message));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> handleTypeMismatchExceptions(
      MethodArgumentTypeMismatchException ex) {
    log.debug("타입 불일치 예외 발생: {}", ex.getMessage());
    return ApiResponse.error(
        Status.of(
            StatusCode.BAD_REQUEST,
            String.format(
                "파라미터 '%s' 유형이어야 합니다. '%s'",
                ex.getName(),
                Objects.isNull(ex.getRequiredType())
                    ? ex.getName()
                    : ex.getRequiredType().getSimpleName())));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
      MissingServletRequestParameterException ex) {
    log.debug("필수 파라미터 누락 예외 발생: {}", ex.getMessage());
    return ApiResponse.error(Status.of(StatusCode.BAD_REQUEST, ex.getMessage()));
  }

  @ExceptionHandler(NullPointerException.class)
  public ResponseEntity<ApiResponse<Void>> handleNullPointerException(NullPointerException ex) {
    log.warn("Null 참조 예외 발생: {}", ex.getMessage());
    StatusCode resolve = StatusCode.resolve(ex.getMessage());
    return ApiResponse.error(Status.of(resolve));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
    log.warn("예기치 않은 예외 발생: {}", ex.getMessage());
    StatusCode resolve = StatusCode.resolve(ex.getMessage());
    return ApiResponse.error(Status.of(resolve));
  }
}
