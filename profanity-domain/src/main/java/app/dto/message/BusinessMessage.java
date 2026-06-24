package app.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자에게 전달할 다국어 비즈니스 메시지")
public interface BusinessMessage {
  @Schema(description = "영문 메시지", example = "Requested successfully")
  String getEngMessage();

  @Schema(description = "국문 메시지", example = "요청에 성공했습니다")
  String getKorMessage();

  @Schema(description = "비즈니스 처리 성공 여부", example = "true")
  Boolean getResult();
}
