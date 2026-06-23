package app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "신규 클라이언트 등록 결과")
public record ClientsRegistResponse(
    @Schema(description = "등록된 이름 또는 조직명", example = "샘플 프로젝트") String name,
    @Schema(description = "API Key 발급에 사용한 이메일", example = "user@example.com") String email,
    @Schema(description = "발급된 API Key", example = "pf_sample_issued_api_key") String apiKey,
    @Schema(description = "등록 메모", example = "검증 환경에서 사용") String note) {}
