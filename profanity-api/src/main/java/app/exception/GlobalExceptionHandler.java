package app.exception;

import app.core.data.response.ApiResponse;
import app.core.data.response.Status;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("비즈니스 예외 발생: {}", ex.getMessage());
        StatusCode resolve = StatusCode.resolve(ex.getMessage());
        return ApiResponse.error(Status.of(resolve));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("인증 정보 예외 발생: {}", ex.getMessage());
        StatusCode resolve = StatusCode.resolve(ex.getMessage());
        return ApiResponse.error(Status.of(resolve));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("인증 예외 발생: {}", ex.getMessage());
        StatusCode resolve = StatusCode.resolve(ex.getMessage());
        return ApiResponse.error(Status.of(resolve));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("접근 권한 예외 발생: {}", ex.getMessage());
        StatusCode resolve = StatusCode.resolve(ex.getMessage());
        return ApiResponse.error(Status.of(resolve));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.debug("유효성 검증 예외 발생: {}", ex.getMessage());
        return ApiResponse.error(Status.of(StatusCode.BAD_REQUEST, ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchExceptions(MethodArgumentTypeMismatchException ex) {
        log.debug("타입 불일치 예외 발생: {}", ex.getMessage());
        return ApiResponse.error(Status.of(StatusCode.BAD_REQUEST,
                String.format("파라미터 '%s' 유형이어야 합니다. '%s'",
                        ex.getName(),
                        Objects.isNull(ex.getRequiredType()) ? ex.getName() : ex.getRequiredType().getSimpleName()
                )));
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
