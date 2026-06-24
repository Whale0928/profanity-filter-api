package app.core.data.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 검출된 단어에 대한 정보를 담는 클래스 */
@Schema(description = "필터에서 검출된 단어 정보")
public record Detected(
    @Schema(description = "검출된 단어의 문자 길이", example = "5") int length,
    @Schema(description = "검출된 원문 단어", example = "나쁜말샘플") String filteredWord) {
  public static Detected of(int length, String filteredWord) {
    return new Detected(length, filteredWord);
  }
}
