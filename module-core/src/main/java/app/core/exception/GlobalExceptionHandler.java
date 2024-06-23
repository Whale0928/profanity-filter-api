package app.core.exception;

import app.core.data.response.ApiResponse;
import app.core.data.response.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static app.core.data.response.constant.StatusCode.BAD_REQUEST;
import static app.core.data.response.constant.StatusCode.SERVICE_UNAVAILABLE;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception ex) {
        log.error("Exception 예외 발생 : ", ex);
        return ApiResponse.error(UUID.randomUUID(), Status.of(SERVICE_UNAVAILABLE));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse> handleNullPointerException(NullPointerException ex) {
        log.warn("NullPointerException 예외 발생 : ", ex);
        return ApiResponse.error(UUID.randomUUID(), Status.of(SERVICE_UNAVAILABLE));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("예외 발생 : ", ex);
        List<String> errorMessages = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());
        return ApiResponse.error(UUID.randomUUID(), Status.of(BAD_REQUEST, errorMessages));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> handleTypeMismatchExceptions(MethodArgumentTypeMismatchException ex) {
        log.warn("예외 발생 : ", ex);
        String errorMessage = String.format("파라미터 '%s' 유형이어야 합니다. '%s'", ex.getName(), ex.getRequiredType().getSimpleName());
        return ApiResponse.error(UUID.randomUUID(), Status.of(BAD_REQUEST, errorMessage));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("예외 발생 : ", ex);
        return ApiResponse.error(UUID.randomUUID(), Status.of(BAD_REQUEST, ex.getMessage()));
    }
}
