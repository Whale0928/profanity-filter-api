package app.dto.request.admin;

import jakarta.validation.constraints.Size;

/**
 * API Key 폐기 요청입니다.
 *
 * @param reason 폐기 사유. 감사 기록과 api_keys.revocation_reason에 남습니다.
 */
public record RevokeApiKeyRequest(@Size(max = 500) String reason) {}
