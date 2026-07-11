package app.application.auth;

import java.time.Instant;
import java.util.UUID;

public record LoginExchangeCodeIssue(UUID codeId, UUID userId, Instant expiresAt) {}
