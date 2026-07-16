package app.domain.apikey;

import java.util.List;
import java.util.UUID;

public record ApiKeyMetadata(
    UUID id,
    String email,
    String issuerInfo,
    List<String> permissions,
    String issuedAt,
    String keyHash) {}
