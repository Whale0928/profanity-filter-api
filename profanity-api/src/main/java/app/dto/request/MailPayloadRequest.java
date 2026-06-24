package app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이메일 인증 코드 검증 요청")
public record MailPayloadRequest(
    @Schema(description = "인증할 이메일", example = "user@example.com")
        @Email(message = "유효한 이메일 형식이 아닙니다")
        @NotBlank(message = "이메일은 필수 입력값입니다")
        String email,
    @Schema(description = "이메일로 발송된 인증 코드", example = "123456")
        @NotBlank(message = "인증 코드는 필수 입력값입니다")
        String code) {}
