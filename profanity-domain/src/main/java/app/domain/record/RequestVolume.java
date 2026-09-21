package app.domain.record;

/**
 * 기간 내 요청 집계입니다.
 *
 * <p>검출 여부는 words가 null이 아니고 빈 문자열도 아닌 행으로 판정합니다. TrackingRecorder가 검출 단어를 잘라낸 뒤 이어 붙이므로 검출이 없으면 빈
 * 문자열이 저장되고, V4 이전에 쌓인 행에는 null이 들어 있을 수 있습니다.
 *
 * @param totalRequests 기록된 총 요청 수
 * @param detectedRequests 비속어가 검출된 요청 수
 */
public record RequestVolume(long totalRequests, long detectedRequests) {

  public static RequestVolume empty() {
    return new RequestVolume(0L, 0L);
  }
}
