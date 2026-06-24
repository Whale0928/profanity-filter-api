package app.dto.response;

import app.dto.message.BusinessMessage;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "요청 처리 결과 메시지")
public record MessageResponse(
    @Schema(description = "요청 처리 성공 여부", example = "true") boolean result,
    @Schema(description = "사용자에게 전달할 다국어 메시지") BusinessMessage message) {

  public static MessageResponse of(BusinessMessage message) {
    return new MessageResponse(message.getResult(), message);
  }
}
