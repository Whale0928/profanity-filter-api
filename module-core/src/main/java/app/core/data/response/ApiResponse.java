package app.core.data.response;

import lombok.Builder;

public record ApiResponse(
        String trackingId,
        Status status,
        Detected[] detected,
        String filtered,
        Elapsed elapsed
) {
    @Builder
    public ApiResponse {
    }
}
