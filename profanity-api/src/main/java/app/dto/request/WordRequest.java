package app.dto.request;

import app.application.EnumValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Schema(description = "단어 추가, 삭제, 수정 요청")
public record WordRequest(
    @Schema(description = "요청할 단어", example = "나쁜말샘플") @NotBlank(message = "요청할 단어는 필수입니다")
        String word,
    @Schema(description = "단어 변경 요청 사유", example = "서비스 정책상 필터링이 필요합니다.")
        @Size(max = 500, message = "추가 설명은 최대 500자까지 가능합니다")
        @NotBlank(message = "요청 사유는 필수입니다")
        String reason,
    @Schema(
            description = "단어 심각도. LOW는 낮은 수준, MEDIUM은 중간 수준, HIGH는 높은 수준입니다.",
            allowableValues = {"LOW", "MEDIUM", "HIGH"},
            example = "MEDIUM")
        @NotNull(message = "단어의 심각도는 필수입니다")
        @EnumValidator(enumClass = WordSeverity.class, message = "올바르지 않은 심각도입니다")
        WordSeverity severity,
    @Schema(
            description = "요청 타입. ADD는 추가, REMOVE는 삭제, MODIFY는 수정 요청입니다.",
            allowableValues = {"ADD", "REMOVE", "MODIFY"},
            example = "ADD")
        @NotNull(message = "요청 타입은 필수입니다")
        @EnumValidator(enumClass = RequestType.class, message = "올바르지 않은 요청 타입입니다")
        RequestType type) {

  @Getter
  @AllArgsConstructor
  public enum WordSeverity {
    LOW("낮은 수위"),
    MEDIUM("중간 수위"),
    HIGH("높은 수위");
    private final String description;
  }

  @Getter
  @AllArgsConstructor
  public enum RequestType {
    ADD("신규 등록 요청"),
    REMOVE("제외 요청"),
    MODIFY("수정 요청");
    private final String description;
  }
}
