package app.core.data.response;

public record ProfanityResponse(
        Boolean success,
        Integer code,
        String message,
        Boolean isProfane
) {

    public static ProfanityResponse success(ResponseMessage message, Boolean isProfane) {
        return new ProfanityResponse(
                true,
                message.getCode(),
                message.getMessage(),
                isProfane
        );
    }

    public static ProfanityResponse success(Boolean isProfane) {
        return new ProfanityResponse(
                true,
                isProfane ? ResponseMessage.SUCCESS.getCode() : ResponseMessage.NO_PROFANITY.getCode(),
                isProfane ? ResponseMessage.SUCCESS.getMessage() : ResponseMessage.NO_PROFANITY.getMessage(),
                isProfane
        );
    }

    public static ProfanityResponse fail(ResponseMessage message, Boolean isProfane) {
        return new ProfanityResponse(
                false,
                message.getCode(),
                message.getMessage(),
                isProfane
        );
    }

    public static ProfanityResponse fail(ResponseMessage message) {
        return new ProfanityResponse(
                false,
                message.getCode(),
                message.getMessage(),
                null
        );
    }


}
