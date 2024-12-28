package app.dto.response;

import lombok.Builder;

@Builder
public record ClientsRegistResponse(
        String name,
        String email,
        String apiKey,
        String note
) {
}
