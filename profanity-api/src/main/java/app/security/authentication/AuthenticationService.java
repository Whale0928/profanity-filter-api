package app.security.authentication;

import app.core.data.response.constant.StatusCode;
import app.security.filter.RequestCredential;
import app.security.filter.RequestCredentialResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationService {
  private final RequestCredentialResolver credentialResolver;
  private final Map<AuthenticationType, RequestAuthenticator> authenticators;

  public AuthenticationService(
      RequestCredentialResolver credentialResolver, List<RequestAuthenticator> authenticators) {
    this.credentialResolver = credentialResolver;
    EnumMap<AuthenticationType, RequestAuthenticator> indexed =
        new EnumMap<>(AuthenticationType.class);
    for (RequestAuthenticator authenticator : authenticators) {
      RequestAuthenticator duplicate = indexed.put(authenticator.supports(), authenticator);
      if (duplicate != null) {
        throw new IllegalStateException(
            "Multiple authenticators registered for " + authenticator.supports());
      }
    }
    this.authenticators = Map.copyOf(indexed);
  }

  public Authentication getAuthentication(HttpServletRequest request) {
    RequestCredential credential = credentialResolver.resolve(request);
    if (credential.type() == AuthenticationType.OAUTH2_ACCESS_TOKEN) {
      throw new CredentialAuthenticationException(
          HttpStatus.UNAUTHORIZED, StatusCode.OAUTH2_ACCESS_TOKEN_UNSUPPORTED);
    }
    RequestAuthenticator authenticator = authenticators.get(credential.type());
    if (authenticator == null) {
      throw new BadCredentialsException(StatusCode.UNAUTHORIZED.stringCode());
    }
    return authenticator.authenticate(credential);
  }
}
