package app.core.data.response;

import app.core.data.response.constant.StatusCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** API 응답 상태에 대한 정보를 담는 클래스 */
@Schema(description = "API 처리 상태")
public record Status(
    @Schema(description = "서비스 내부 비즈니스 상태 코드", example = "2000") Integer code,
    @Schema(description = "서비스 내부 상태 이름", example = "Ok") String message,
    @Schema(description = "상태 코드에 대한 기본 설명", example = "정상적으로 처리 되었습니다.") String description,
    @Schema(description = "요청별 상세 오류 설명. 상세 내용이 없으면 빈 문자열입니다.", example = "")
        String DetailDescription) {
  public static Status of(StatusCode code) {
    return new Status(code.code(), code.status(), code.description(), "");
  }

  public static Status of(StatusCode code, String detailDescription) {
    return new Status(code.code(), code.status(), code.description(), detailDescription);
  }

  public static Status of(StatusCode code, List<String> detailDescription) {
    return new Status(
        code.code(),
        code.status(),
        code.description(),
        detailDescription.stream().map(String::toString).reduce("", (a, b) -> a + b + "  / "));
  }
}
