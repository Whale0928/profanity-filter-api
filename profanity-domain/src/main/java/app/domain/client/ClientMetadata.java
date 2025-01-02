package app.domain.client;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ClientMetadata(
        UUID id,
        String email,
        String issuerInfo,
        String note,
        List<String> permissions,
        String issuedAt
) {
}
