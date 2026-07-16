package app.security.authentication;

import java.util.List;
import java.util.UUID;

/** 외부 API Key 인증 주체입니다. API Key 원문은 포함하지 않습니다. */
public record ApiKeyPrincipal(
    UUID id,
    String email,
    String issuerInfo,
    List<String> permissions,
    String issuedAt,
    String keyHash)
    implements ServicePrincipal {

  public ApiKeyPrincipal {
    permissions = List.copyOf(permissions);
  }

  @Override
  public AuthenticationType authenticationType() {
    return AuthenticationType.API_KEY;
  }
}
