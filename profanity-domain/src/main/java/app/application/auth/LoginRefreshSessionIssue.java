package app.application.auth;

import java.time.Instant;
import java.util.UUID;

public record LoginRefreshSessionIssue(
    UUID sessionId, UUID tokenId, UUID userId, Instant tokenExpiresAt, Instant sessionExpiresAt) {}
