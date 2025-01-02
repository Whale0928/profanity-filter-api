package app.security.authentication;

import lombok.Builder;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Builder
public record CustomPrincipal(
        String apiKey,
        UUID id,
        String email,
        String issuerInfo,
        List<String> permissions,
        String issuedAt
) implements Serializable {
    public static CustomPrincipal of(Object principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof CustomPrincipal customPrincipal) {
            return customPrincipal;
        }
        return null;
    }
}
