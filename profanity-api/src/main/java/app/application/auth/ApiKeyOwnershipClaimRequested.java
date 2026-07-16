package app.application.auth;

import java.util.UUID;

public record ApiKeyOwnershipClaimRequested(UUID userId, String verifiedEmail) {}
