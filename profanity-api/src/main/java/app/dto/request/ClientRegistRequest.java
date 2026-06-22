package app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "신규 클라이언트 등록 요청")
public record ClientRegistRequest(
    @Schema(description = "이름 또는 조직명. 실제 정보 입력을 권장합니다.", example = "openerd")
        @NotBlank(message = "이름(조직명)은 필수 입력값입니다")
        @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요")
        String name,
    @Schema(description = "API Key 발급에 사용할 이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수 입력값입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,
    @Schema(description = "발급자 정보", example = "개인 프로젝트 비속어 필터링")
        @NotBlank(message = "발급자 정보는 필수 입력값입니다")
        @Size(max = 200, message = "발급자 정보는 200자 이하로 입력해주세요")
        String issuerInfo,
    @Schema(description = "메모", example = "테스트 환경에서 사용")
        @Size(max = 500, message = "메모는 500자 이하로 입력해주세요")
        String note) {
  public ClientRegistCommand toCommand() {
    return ClientRegistCommand.from(name(), email(), issuerInfo(), note());
  }
}
