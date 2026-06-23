package app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증 코드 검증 결과")
public record EmailVerificationResponse(
    @Schema(description = "인증된 이메일에 연결된 API Key", example = "pf_sample_verified_api_key")
        String apikey) {}
