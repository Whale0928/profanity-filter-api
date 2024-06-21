package app.core.data.response;

import app.core.data.elapsed.Elapsed;
import lombok.Builder;

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
}
