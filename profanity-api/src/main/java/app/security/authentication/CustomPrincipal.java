package app.security.authentication;

import lombok.Builder;

import java.io.Serializable;
import java.util.List;

@Builder
public record CustomPrincipal(
        String email,
        String issuerInfo,
        List<String> permissions,
        String issuedAt
) implements Serializable {
}
