package app.dto.request;

public record ClientRegistRequest(
        String name,
        String email,
        String issuerInfo,
        String note
) {
}
