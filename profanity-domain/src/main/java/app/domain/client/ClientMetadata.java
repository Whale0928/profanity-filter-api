package app.domain.client;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ClientMetadata(
    UUID id,
    String email,
    String issuerInfo,
    String note,
    List<String> permissions,
    String issuedAt) {}
