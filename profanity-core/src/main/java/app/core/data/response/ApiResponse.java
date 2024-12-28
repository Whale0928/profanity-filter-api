package app.core.data.response;

import app.core.data.response.constant.StatusCode;
import lombok.Builder;
import org.springframework.http.ResponseEntity;

public record ApiResponse<T>(
        Status status,
        T data
) {
    @Builder
    private static <T> ApiResponse<T> of(Status status, T data) {
        return new ApiResponse<>(status, data);
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.of(
                Status.of(StatusCode.OK),
                data
        ));
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(Status status) {
        return ResponseEntity.ok(ApiResponse.<T>of(
                status,
                null
        ));
    }
}
