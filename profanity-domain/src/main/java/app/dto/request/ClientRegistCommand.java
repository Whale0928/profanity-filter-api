package app.dto.request;

public record ClientRegistCommand(
        String name,
        String email,
        String issuerInfo,
        String note
) {
    public static ClientRegistCommand from(
            String name, String email, String issuerInfo, String note) {
        return new ClientRegistCommand(name, email, issuerInfo, note);
    }
}
