package app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientUpdateRequest(
        @NotBlank(message = "발급자 정보는 필수 입력값입니다")
        @Size(max = 200, message = "발급자 정보는 200자 이하로 입력해주세요")
        String issuerInfo,

        @Size(max = 500, message = "메모는 500자 이하로 입력해주세요")
        String note
) {
}
