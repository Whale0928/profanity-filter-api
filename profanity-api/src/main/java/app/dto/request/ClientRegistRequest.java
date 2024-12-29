package app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRegistRequest(
        @NotBlank(message = "이름(조직명)은 필수 입력값입니다")
        @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요")
        String name,

        @NotBlank(message = "이메일은 필수 입력값입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @NotBlank(message = "발급자 정보는 필수 입력값입니다")
        @Size(max = 200, message = "발급자 정보는 200자 이하로 입력해주세요")
        String issuerInfo,

        @Size(max = 500, message = "메모는 500자 이하로 입력해주세요")
        String note
) {
    public ClientRegistCommand toCommand() {
        return ClientRegistCommand.from(name(), email(), issuerInfo(), note());
    }
}
