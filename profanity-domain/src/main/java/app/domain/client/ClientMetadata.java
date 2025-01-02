package app.domain.client;

import lombok.Builder;

import java.util.List;

@Builder
public record ClientMetadata(
        String email,
        String issuerInfo,
        String note,
        List<String> permissions,
        String issuedAt
) {
}
