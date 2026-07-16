package app.security.authentication;

import app.application.client.MetadataReader;
import app.domain.client.ClientMetadata;
import app.security.filter.RequestCredential;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticator implements RequestAuthenticator {
  private static final String ROLE_PREFIX = "ROLE_";

  private final MetadataReader clientMetadataReader;

  @Override
  public AuthenticationType supports() {
    return AuthenticationType.API_KEY;
  }

  @Override
  public Authentication authenticate(RequestCredential credential) {
    String apiKey = credential.value();
    ClientMetadata metadata;
    try {
      metadata = clientMetadataReader.read(apiKey);
    } catch (IllegalArgumentException | NoSuchElementException exception) {
      throw new BadCredentialsException(exception.getMessage(), exception);
    }

    ApiKeyPrincipal principal =
        new ApiKeyPrincipal(
            metadata.id(),
            metadata.email(),
            metadata.issuerInfo(),
            metadata.permissions(),
            metadata.issuedAt());
    List<String> authorities = new ArrayList<>();
    authorities.add(AuthenticationType.API_KEY.authority());
    authorities.addAll(
        principal.permissions().stream().map(permission -> ROLE_PREFIX + permission).toList());

    return new CustomAuthentication(
        AuthenticationType.API_KEY,
        apiKey,
        AuthorityUtils.createAuthorityList(authorities),
        principal);
  }
}
