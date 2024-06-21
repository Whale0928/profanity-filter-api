package app.core.data.response;

import app.core.data.elapsed.Elapsed;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

public record ApiResponse(
        UUID trackingId,
        Status status,
        List<Detected> detected,
        String filtered,
        Long elapsed
) {
    @Builder
    public ApiResponse(UUID trackingId, Status status, List<Detected> detected, String filtered, Elapsed elapsed) {
        this(trackingId, status, detected, filtered, elapsed.getMilliseconds());
    }
}