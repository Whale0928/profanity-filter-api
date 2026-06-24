package app.domain.client;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "클라이언트 메타데이터")
public record ClientMetadata(
    @Schema(description = "클라이언트 식별자", example = "018f4fd8-9f6f-7d1a-9b80-3f1f8dd7c001") UUID id,
    @Schema(description = "API Key 발급에 사용한 이메일", example = "user@example.com") String email,
    @Schema(description = "API Key 발급자 정보", example = "비속어 필터링 연동") String issuerInfo,
    @Schema(description = "클라이언트 메모", example = "운영 환경에서 사용") String note,
    @Schema(description = "클라이언트 권한 목록", example = "[\"READ\"]") List<String> permissions,
    @Schema(description = "API Key 발급 시각", example = "2026-06-23T09:00:00") String issuedAt) {}
