package app.data.response;

import org.springframework.http.HttpStatus;

public record ProfanityResponse(
        HttpStatus status,
        String message,
        Boolean isProfane
) {

    private final static String SUCCESS_MESSAGE = "비속어가 존재합니다.";
    private final static String NO_PROFANITY_MESSAGE = "비속어가 존재하지 않습니다.";

    public static ProfanityResponse success(Boolean isProfane) {
        return new ProfanityResponse(HttpStatus.OK, SUCCESS_MESSAGE, isProfane);
    }

    public static ProfanityResponse fail(Boolean isProfane) {
        return new ProfanityResponse(HttpStatus.BAD_REQUEST, NO_PROFANITY_MESSAGE, isProfane);
    }
}
