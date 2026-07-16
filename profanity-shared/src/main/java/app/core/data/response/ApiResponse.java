package app.core.data.response;

import app.core.data.response.constant.StatusCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@Schema(description = "공통 API 응답 래퍼")
public record ApiResponse<T>(
    @Schema(description = "비즈니스 상태 코드와 메시지") Status status,
    @Schema(description = "엔드포인트별 응답 데이터. 오류 응답에서는 null일 수 있습니다.") T data,
    @Schema(description = "응답 커스터마이징 메타데이터. 값이 없으면 응답에서 생략됩니다.")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> meta) {
  @Builder
  private static <T> ApiResponse<T> of(Status status, T data) {
    return new ApiResponse<>(status, data, new HashMap<>());
  }

  public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
    return ResponseEntity.ok(ApiResponse.of(Status.of(StatusCode.OK), data));
  }

  public static <T> ResponseEntity<ApiResponse<T>> error(Status status) {
    return ResponseEntity.ok(ApiResponse.<T>of(status, null));
  }

  public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatusCode httpStatus, Status status) {
    return ResponseEntity.status(httpStatus).body(ApiResponse.<T>of(status, null));
  }
}
