package app.dto.message;


public enum ErrorBusinessMessage implements BusinessMessage {
    NOT_FOUND("Resource not found", "리소스를 찾을 수 없습니다", false),
    INVALID_REQUEST("Invalid request", "잘못된 요청입니다", false),
    SERVER_ERROR("Server error occurred", "서버 에러가 발생했습니다", false);

    private final String engMessage;
    private final String korMessage;
    private final Boolean result;

    ErrorBusinessMessage(String engMessage, String korMessage, Boolean result) {
        this.engMessage = engMessage;
        this.korMessage = korMessage;
        this.result = result;
    }

    @Override
    public String getEngMessage() {
        return this.engMessage;
    }

    @Override
    public String getKorMessage() {
        return this.korMessage;
    }

    @Override
    public Boolean getResult() {
        return this.result;
    }
}
