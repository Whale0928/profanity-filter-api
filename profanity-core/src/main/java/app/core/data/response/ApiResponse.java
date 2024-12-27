package app.core.data.response;

import app.core.data.elapsed.Elapsed;
import lombok.Builder;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public record ApiResponse(
        UUID trackingId,
        Status status,
        Set<Detected> detected,
        String filtered,
        String elapsed
) {
    @Builder
    public ApiResponse(UUID trackingId, Status status, Set<Detected> detected, String filtered, Elapsed elapsed) {
        this(trackingId, status, detected, filtered, elapsed.toString());
    }

    public static ResponseEntity<ApiResponse> error(UUID trackingId, Status status) {
        return ResponseEntity.ok(ApiResponse.builder()
                .trackingId(trackingId)
                .status(status)
                .detected(Collections.emptySet())
                .filtered("")
                .elapsed(Elapsed.zero())
                .build());

    }
}
