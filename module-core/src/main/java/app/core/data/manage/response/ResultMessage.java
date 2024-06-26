package app.core.data.manage.response;

public enum ResultMessage {
    SUCCESS_SYNC_WORD("성공적으로 동기화 되었습니다."),
    FAIL_SYNC_WORD("동기화에 실패하였습니다.");

    private final String message;

    ResultMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
