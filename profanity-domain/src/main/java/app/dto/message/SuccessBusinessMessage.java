package app.dto.message;

public enum SuccessBusinessMessage implements BusinessMessage {
    CREATE_SUCCESS("Created successfully", "생성에 성공했습니다", true),
    REQUEST_SUCCESS("Requested successfully", "요청에 성공했습니다", true),
    UPDATE_SUCCESS("Updated successfully", "수정에 성공했습니다", true),
    DELETE_SUCCESS("Deleted successfully", "삭제에 성공했습니다", true);

    private final String engMessage;
    private final String korMessage;
    private final Boolean result;

    SuccessBusinessMessage(String engMessage, String korMessage, Boolean result) {
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
