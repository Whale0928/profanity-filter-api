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
        Elapsed elapsed
) {
    @Builder
    public ApiResponse {
    }
}
