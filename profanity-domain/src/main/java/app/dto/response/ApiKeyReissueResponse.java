package app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API Key 재발급 결과")
public record ApiKeyReissueResponse(
    @Schema(description = "새로 발급된 API Key", example = "pf_sample_reissued_api_key")
        String newApiKey) {}
