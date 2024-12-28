package app.core.exception;

import app.core.data.response.ApiResponse;
import app.core.data.response.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;

import static app.core.data.response.constant.StatusCode.BAD_REQUEST;
import static app.core.data.response.constant.StatusCode.SERVICE_UNAVAILABLE;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("BusinessException 발생: ", ex);
        return ApiResponse.error(ex.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Exception 예외 발생: ", ex);
        return ApiResponse.error(Status.of(SERVICE_UNAVAILABLE));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointerException(NullPointerException ex) {
        log.warn("NullPointerException 예외 발생: ", ex);
        return ApiResponse.error(Status.of(SERVICE_UNAVAILABLE));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Validation 예외 발생: ", ex);
        return ApiResponse.error(Status.of(BAD_REQUEST, ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchExceptions(MethodArgumentTypeMismatchException ex) {
        log.warn("TypeMismatch 예외 발생: ", ex);
        return ApiResponse.error(Status.of(BAD_REQUEST, String.format("파라미터 '%s' 유형이어야 합니다. '%s'", ex.getName(), Objects.isNull(ex.getRequiredType()) ? ex.getName() : ex.getRequiredType().getSimpleName())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        log.warn("Parameter 누락 예외 발생: ", ex);
        return ApiResponse.error(Status.of(BAD_REQUEST, ex.getMessage()));
    }
}
