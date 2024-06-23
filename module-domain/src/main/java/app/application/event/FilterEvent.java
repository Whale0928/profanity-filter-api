package app.application.event;

import app.core.data.response.ApiResponse;
import app.core.data.response.Detected;
import app.dto.request.FilterRequest;

import java.util.Set;
import java.util.UUID;

public record FilterEvent(
        UUID trackingId,
        String apiKey,
        Set<Detected> words,
        String referrer,
        String ip
) {
    public static FilterEvent create(
            FilterRequest filterRequest,
            ApiResponse apiResponse
    ) {
        return new FilterEvent(
                apiResponse.trackingId(),
                filterRequest.apiKey(),
                apiResponse.detected(),
                filterRequest.referrer(),
                filterRequest.clientIp()
        );
    }
}
