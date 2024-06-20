package app.core.data.response;

public record Status(
        Integer code,
        String message,
        String description,
        String DetailDescription
) {
    public static Status of(StatusCode code) {
        return new Status(code.code(), code.status(), code.description(), "");
    }

    public static Status of(StatusCode code, String detailDescription) {
        return new Status(code.code(), code.status(), code.description(), detailDescription);
    }
}
