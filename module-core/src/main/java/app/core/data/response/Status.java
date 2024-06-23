package app.core.data.response;

import app.core.data.response.constant.StatusCode;

import java.util.List;

/**
 * API 응답 상태에 대한 정보를 담는 클래스
 */
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

    public static Status of(StatusCode code, List<String> detailDescription) {
        return new Status(code.code(), code.status(), code.description(), detailDescription.stream().map(String::toString).reduce("", (a, b) -> a + b + "  / "));
    }
}
