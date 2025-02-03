package app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WordRequest(
        @NotBlank(message = "요청할 단어는 필수입니다")
        String word,

        @NotBlank(message = "요청 사유는 필수입니다")
        String reason,

        @NotNull(message = "단어의 심각도는 필수입니다")
        WordSeverity severity,

        @Size(max = 500, message = "추가 설명은 최대 500자까지 가능합니다")
        String description,

        @NotNull(message = "요청 타입은 필수입니다")
        RequestType type // ADD, MODIFY, REMOVE 등
) {
    public enum WordSeverity {
        LOW,     // 낮은 수위
        MEDIUM,  // 중간 수위
        HIGH     // 높은 수위
    }

    public enum RequestType {
        ADD,     // 신규 추가
        MODIFY,  // 기존 단어 수정
        REMOVE   // 제거 요청
    }
}
