package app.dto.request;

import app.core.data.constant.Mode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
@Schema(description = "비속어 필터링 요청")
public record ApiRequest(
    @Schema(description = "필터링 대상 문자열", example = "안녕하세요. 검증할 문장입니다.")
        @NotBlank(message = "필터링 대상 문자열은 필요합니다.")
        String text,
    @Schema(
            description =
                "필터링 모드. QUICK은 원색적인 표현만 간략히 검증하고, NORMAL은 모든 비속어를 검증하며, FILTER는 검출 단어를 마스킹합니다.",
            allowableValues = {"QUICK", "NORMAL", "FILTER"},
            example = "FILTER")
        @NotNull(message = "필터링 모드는 필수입니다.")
        Mode mode,
    @Pattern(
            regexp =
                "((http[s]?|ftp):\\/\\/)?(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=가-힣]{1,256}[:|\\.][a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+,.~#?&\\/=가-힣]*)",
            message = "콜백 URL 형식이 올바르지 않습니다.")
        @Schema(
            description = "비동기 처리 결과를 받을 callback URL. 비어 있으면 동기 요청으로 처리합니다.",
            example = "https://example.com/callback")
        String callbackUrl) {

  /**
   * 비동기 요청 여부를 반환합니다.
   *
   * @return the boolean
   */
  @JsonIgnore
  public Boolean isAsync() {
    return callbackUrl != null && !callbackUrl.trim().isEmpty();
  }
}
