package app.application.inquiry;

/**
 * 문의 요청자의 표시 정보입니다.
 *
 * <p>로그인 사용자가 등록한 문의는 사용자 계정의 이름과 대표 이메일을 사용합니다. 기존 외부 API로 들어온 문의는 사용자 계정을 확인할 수 없는 경우가 있어 API
 * Key의 이름과 발급 이메일을 대신 사용합니다. 어느 쪽도 확인되지 않으면 두 값 모두 비어 있습니다.
 */
public record InquiryRequester(String name, String email) {

  private static final InquiryRequester UNKNOWN = new InquiryRequester(null, null);

  public static InquiryRequester unknown() {
    return UNKNOWN;
  }
}
