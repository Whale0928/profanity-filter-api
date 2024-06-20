package app.core.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Builder
public record ErrorResponse(
        String message,
        HttpStatus status,
        LocalDateTime timestamp
) {
}
