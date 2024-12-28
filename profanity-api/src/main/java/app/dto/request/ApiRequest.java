package app.dto.request;

import app.core.data.constant.Mode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ApiRequest(
        @NotBlank(message = "필터링 대상 문자열은 필요합니다.")
        String text,
        @NotNull(message = "필터링 모드는 필수입니다.")
        Mode mode,
        @Pattern(regexp = "((http[s]?|ftp):\\/\\/)?(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=가-힣]{1,256}[:|\\.][a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+,.~#?&\\/=가-힣]*)", message = "콜백 URL 형식이 올바르지 않습니다.")
        String callbackUrl
) {

    /**
     * 비동기 요청 여부를 반환합니다.
     *
     * @return the boolean
     */
    @JsonIgnore
    public Boolean isAsync() {
        return callbackUrl != null;
    }
}
