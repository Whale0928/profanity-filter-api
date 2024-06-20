package app.core.data.response;

public enum StatusCode {

    OK(2000, "정상적으로 처리 되었습니다."),
    ACCEPTED(2020, "요청이 접수 되었습니다. 처리가 완료 시 결과를 받을 수 확인할 수 있습니다. ( Tracking Id를 이용해 값을 비교할 수 있습니다. )"),
    BAD_REQUEST(4000, "처리에 실패하였습니다. 요청이 잘못 되었거나 필수 파라미터가 누락된 경우 발생 합니다. Description에서 보다 상세한 오류 메세지를 확인할 수 있습니다."),
    UNAUTHORIZED(4010, "인증 키가 누락 되었습니다."),
    FORBIDDEN(4030, "인증 권한이 부적절합니다. 인증 키가 유효하지 않거나 권한이 없는 경우 발생합니다."),
    TOO_MANY_REQUESTS(4290, "요청 횟수가 제한이 초과 되었습니다. 일정 시간이 지나면 다시 시도해 주세요."),
    INTERNAL_SERVER_ERROR(5000, "서버 내부 오류가 발생 했습니다., 재시도 시 문제가 지속 되는 경우 관리자에게 문의 바랍니다."),
    SERVICE_UNAVAILABLE(5030, "현재 서비스가 점검중이므로 서비스 응답할 수 없는 경우 발생합니다.");

    private final int code;
    private final String description;

    StatusCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public String status() {
        return this.name().charAt(0) + this.name().substring(1).toLowerCase();
    }

    public int code() {
        return code;
    }

    public String description() {
        return description;
    }
}
