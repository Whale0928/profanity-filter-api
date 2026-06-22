package app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "클라이언트 정보 수정 요청")
public record ClientUpdateRequest(
    @Schema(description = "수정할 발급자 정보", example = "운영 서비스 비속어 필터링")
        @NotBlank(message = "발급자 정보는 필수 입력값입니다")
        @Size(max = 200, message = "발급자 정보는 200자 이하로 입력해주세요")
        String issuerInfo,
    @Schema(description = "수정할 메모", example = "운영 환경에서 사용")
        @Size(max = 500, message = "메모는 500자 이하로 입력해주세요")
        String note) {}
