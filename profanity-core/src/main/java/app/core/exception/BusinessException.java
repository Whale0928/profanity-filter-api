package app.core.exception;


import app.core.data.response.Status;
import app.core.data.response.constant.StatusCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Status status;

    public BusinessException(StatusCode statusCode) {
        this(statusCode, "");
    }

    public BusinessException(StatusCode statusCode, String detailDescription) {
        super(statusCode.description());
        this.status = Status.of(statusCode, detailDescription);
    }

}
