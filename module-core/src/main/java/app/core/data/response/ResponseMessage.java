package app.core.data.response;

public enum ResponseMessage {
    SUCCESS(200, "비속어가 존재합니다."),
    NO_PROFANITY(200, "비속어가 존재하지 않습니다."),
    BAD_REQUEST(400, "잘못된 요청입니다."),
    ERROR(500, "비속어 검사에 실패했습니다.");

    private final int code;
    private final String message;

    ResponseMessage(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
