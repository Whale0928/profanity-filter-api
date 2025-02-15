package app.dto.response;

import app.dto.message.BusinessMessage;

public record MessageResponse(
        boolean result,
        BusinessMessage message
) {

    public static MessageResponse of(BusinessMessage message) {
        return new MessageResponse(message.getResult(), message);
    }
}
