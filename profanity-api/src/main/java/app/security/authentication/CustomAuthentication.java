package app.security.authentication;

import java.util.Collection;
import java.util.Objects;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class CustomAuthentication extends AbstractAuthenticationToken {

  private final AuthenticationType authenticationType;
  private final ServicePrincipal principal;
  private Object credentials;

  public CustomAuthentication(
      AuthenticationType authenticationType,
      Object credentials,
      Collection<? extends GrantedAuthority> authorities,
      ServicePrincipal principal) {
    super(authorities);
    this.authenticationType = Objects.requireNonNull(authenticationType);
    this.credentials = credentials;
    this.principal = Objects.requireNonNull(principal);
    if (authenticationType != principal.authenticationType()) {
      throw new IllegalArgumentException("Authentication type and principal type must match");
    }
    setAuthenticated(true);
  }

  public AuthenticationType authenticationType() {
    return authenticationType;
  }

  @Override
  public Object getCredentials() {
    return credentials;
  }

  @Override
  public ServicePrincipal getPrincipal() {
    return principal;
  }

  @Override
  public void eraseCredentials() {
    super.eraseCredentials();
    credentials = null;
  }

  @Override
  public String toString() {
    return "CustomAuthentication[type="
        + authenticationType
        + ", principalId="
        + principal.id()
        + ", authenticated="
        + isAuthenticated()
        + "]";
  }
}
