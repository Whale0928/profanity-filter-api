package app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;

public record WordRequest(
        @NotBlank(message = "요청할 단어는 필수입니다")
        String word,

        @Size(max = 500, message = "추가 설명은 최대 500자까지 가능합니다")
        @NotBlank(message = "요청 사유는 필수입니다")
        String reason,

        @NotBlank(message = "단어의 심각도는 필수입니다")
        @NotNull(message = "단어의 심각도는 필수입니다")
        WordSeverity severity,

        @NotBlank(message = "요청 타입은 필수입니다")
        @NotNull(message = "요청 타입은 필수입니다")
        RequestType type
) {

    @AllArgsConstructor
    public enum WordSeverity {
        LOW("낮은 수위"),
        MEDIUM("중간 수위"),
        HIGH("높은 수위");
        private final String description;
    }

    @AllArgsConstructor
    public enum RequestType {
        ADD("신규 등록 요청"),
        REMOVE("제외 요청"),
        MODIFY("수정 요청");
        private final String description;
    }
}
