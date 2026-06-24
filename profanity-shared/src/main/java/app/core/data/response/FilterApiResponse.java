package app.core.data.response;

import app.core.data.elapsed.Elapsed;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import org.springframework.http.ResponseEntity;

@Schema(description = "비속어 필터링 응답")
public record FilterApiResponse(
    @Schema(description = "요청 추적 ID", example = "018f4fd8-9f6f-7d1a-9b80-3f1f8dd7c002")
        UUID trackingId,
    @Schema(description = "필터링 처리 상태") Status status,
    @Schema(description = "검출된 단어 목록. 검출 결과가 없으면 빈 배열입니다.") Set<Detected> detected,
    @Schema(description = "FILTER 또는 advanced 처리 후 마스킹된 문장", example = "문장 안에 ***** 이 포함된다")
        String filtered,
    @Schema(description = "필터 처리 소요 시간", example = "0.00000000 s / 0.00000 ms / 0.000 µs")
        String elapsed) {
  @Builder
  public FilterApiResponse(
      UUID trackingId, Status status, Set<Detected> detected, String filtered, Elapsed elapsed) {
    this(trackingId, status, detected, filtered, elapsed.toString());
  }

  public static ResponseEntity<FilterApiResponse> error(UUID trackingId, Status status) {
    return ResponseEntity.ok(
        FilterApiResponse.builder()
            .trackingId(trackingId)
            .status(status)
            .detected(Collections.emptySet())
            .filtered("")
            .elapsed(Elapsed.zero())
            .build());
  }
}
