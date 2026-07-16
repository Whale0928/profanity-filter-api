package app.dto.response;

import java.util.UUID;

public record LoginUserResponse(UUID id, String displayName, String email, String avatarUrl) {}
