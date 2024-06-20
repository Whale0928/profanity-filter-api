package app.request;

import app.core.data.constant.Mode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApiRequest(
        @NotBlank(message = "필터링 대상 문자열은 필요합니다.")
        String text,
        @NotNull(message = "필터링 모드는 필수입니다.")
        Mode mode
) {
}
