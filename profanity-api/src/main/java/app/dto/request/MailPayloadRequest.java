package app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MailPayloadRequest(
        @Email(message = "유효한 이메일 형식이 아닙니다")
        @NotBlank(message = "이메일은 필수 입력값입니다")
        String email,
        @NotBlank(message = "인증 코드는 필수 입력값입니다")
        String code
) {
}
