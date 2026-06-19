package app.security.authentication;

import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

@Slf4j
@EqualsAndHashCode(callSuper = false)
public class CustomAuthentication extends AbstractAuthenticationToken {

  private final String apiKey;
  private final CustomPrincipal details;

  public CustomAuthentication(
      String apiKey, Collection<? extends GrantedAuthority> authorities, CustomPrincipal details) {
    super(authorities);
    this.apiKey = apiKey;
    this.details = details;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return apiKey;
  }

  @Override
  public Object getPrincipal() {
    return details;
  }
}
