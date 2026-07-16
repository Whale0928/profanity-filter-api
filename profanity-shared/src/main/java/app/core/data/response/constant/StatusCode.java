package app.core.data.response.constant;

import java.util.Arrays;

public enum StatusCode {
  OK(2000, "정상적으로 처리 되었습니다."),
  ACCEPTED(2020, "요청이 접수 되었습니다. 처리가 완료 시 결과를 받을 수 확인할 수 있습니다."),
  PROCESSING(2021, "처리 진행 중입니다. 완료 시 결과를 확인할 수 있습니다."),
  BAD_REQUEST(
      4000,
      "처리에 실패하였습니다. 요청이 잘못 되었거나 필수 파라미터가 누락된 경우 발생 합니다. Description에서 보다 상세한 오류 메세지를 확인할 수 있습니다."),
  NOT_FOUNT_TRACKING_ID(4003, "유효하지 않은 Tracking ID 입니다. Tracking ID를 확인해 주세요."),
  UNAUTHORIZED(4010, "인증 키가 누락 되었습니다."),
  OAUTH2_LOGIN_FAILED(4011, "OAuth2 로그인에 실패했습니다."),
  LOGIN_CODE_INVALID(4012, "로그인 교환 코드가 유효하지 않습니다."),
  LOGIN_TOKEN_INVALID(4013, "로그인 access token이 유효하지 않습니다."),
  LOGIN_TOKEN_EXPIRED(4014, "로그인 access token이 만료되었습니다."),
  REFRESH_TOKEN_INVALID(4015, "로그인 refresh token이 유효하지 않습니다."),
  REFRESH_TOKEN_REUSED(4016, "이미 사용한 refresh token이 다시 제출되었습니다."),
  OAUTH2_ACCESS_TOKEN_UNSUPPORTED(4017, "OAuth2 access token 인증은 아직 지원하지 않습니다."),
  FORBIDDEN(4030, "인증 권한이 부적절합니다. 인증 키가 유효하지 않거나 권한이 없는 경우 발생합니다."),
  NOT_FOUND_CLIENT(4031, "클라이언트 정보를 찾을 수 없습니다. 인증 키가 유효하지 않거나 권한이 없는 경우 발생합니다."),
  INVALID_API_KEY(4032, "API 키가 유효하지 않습니다. 인증 키가 유효하지 않거나 권한이 없는 경우 발생합니다."),
  USER_INACTIVE(4033, "비활성 사용자 계정입니다."),
  TOO_MANY_REQUESTS(4290, "요청 횟수가 제한이 초과 되었습니다. 일정 시간이 지나면 다시 시도해 주세요."),
  INVALID_TRACKING_ID(4002, "유효하지 않은 Tracking ID 입니다. Tracking ID를 확인해 주세요."),
  INVALID_CALLBACK_URL(4001, "콜백 URL 형식이 올바르지 않습니다. 콜백 URL을 확인해 주세요."),
  AMBIGUOUS_CREDENTIALS(4004, "하나의 요청에는 하나의 인증 정보만 전달할 수 있습니다."),
  API_KEY_ALREADY_EXPIRED(4005, "이미 만료된 API Key입니다."),
  API_KEY_NOT_FOUND(4040, "API Key를 찾을 수 없습니다."),
  INTERNAL_SERVER_ERROR(5000, "서버 내부 오류가 발생 했습니다., 재시도 시 문제가 지속 되는 경우 관리자에게 문의 바랍니다."),
  SERVICE_UNAVAILABLE(5030, "현재 서비스가 점검중이므로 서비스 응답할 수 없는 경우 발생합니다."),
  ;

  private final int code;
  private final String description;

  StatusCode(int code, String description) {
    this.code = code;
    this.description = description;
  }

  public static StatusCode resolve(String message) {
    if (message == null) return INTERNAL_SERVER_ERROR;

    try {
      // 1차: 숫자 코드로 찾기
      if (message.matches("\\d+")) {
        int code = Integer.parseInt(message);
        return Arrays.stream(values())
            .filter(statusCode -> statusCode.code == code)
            .findFirst()
            .orElse(INTERNAL_SERVER_ERROR);
      }

      // 2차: enum 이름으로 찾기
      return Arrays.stream(values())
          .filter(statusCode -> message.contains(statusCode.name()))
          .findFirst()
          .orElse(INTERNAL_SERVER_ERROR);

    } catch (Exception e) {
      return INTERNAL_SERVER_ERROR;
    }
  }

  public String status() {
    return this.name().charAt(0) + this.name().substring(1).toLowerCase();
  }

  public int code() {
    return code;
  }

  public String stringCode() {
    return String.valueOf(code);
  }

  public String description() {
    return description;
  }
}
