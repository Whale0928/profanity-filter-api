package app.application.event;

import app.core.data.constant.Mode;
import app.core.data.response.Detected;
import app.core.data.response.FilterApiResponse;
import app.dto.request.FilterRequest;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record FilterEvent(
        UUID trackingId,
        Mode mode,
        String apiKey,
        String requestText,
        Set<String> words,
        String referrer,
        String ip
) {
    public static FilterEvent create(
            FilterRequest filterRequest,
            FilterApiResponse filterApiResponse
    ) {
        return new FilterEvent(
                filterApiResponse.trackingId(),
                filterRequest.mode(),
                filterRequest.apiKey(),
                filterRequest.text(),
                filterApiResponse.detected().stream().map(Detected::filteredWord).collect(Collectors.toSet()),
                filterRequest.referrer(),
                filterRequest.clientIp()
        );
    }
}
