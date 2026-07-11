package app.security.filter;

import static app.core.data.response.constant.StatusCode.AMBIGUOUS_CREDENTIALS;
import static app.core.data.response.constant.StatusCode.LOGIN_TOKEN_INVALID;
import static app.core.data.response.constant.StatusCode.UNAUTHORIZED;

import app.security.authentication.AuthenticationType;
import app.security.authentication.CredentialAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestCredentialResolver {
  public static final String API_KEY_HEADER = "X-API-KEY";
  private static final String BEARER_SCHEME = "Bearer";

  private final AuthenticationRoutePolicy routePolicy;

  public RequestCredential resolve(HttpServletRequest request) {
    rejectConflicts(request);
    List<String> apiKeyHeaders = headerValues(request, API_KEY_HEADER);
    List<String> authorizationHeaders = headerValues(request, HttpHeaders.AUTHORIZATION);
    boolean hasApiKeyHeader = !apiKeyHeaders.isEmpty();
    boolean hasAuthorizationHeader = !authorizationHeaders.isEmpty();

    AuthenticationRoutePolicy.Route route = routePolicy.route(request);
    if (hasApiKeyHeader) {
      if (!route.apiKeyAllowed()) {
        throw new CredentialAuthenticationException(HttpStatus.UNAUTHORIZED, LOGIN_TOKEN_INVALID);
      }
      String apiKey = apiKeyHeaders.get(0);
      if (apiKey == null || apiKey.isBlank()) {
        throw new BadCredentialsException(UNAUTHORIZED.stringCode());
      }
      return new RequestCredential(AuthenticationType.API_KEY, apiKey);
    }

    if (hasAuthorizationHeader) {
      return bearerCredential(authorizationHeaders.get(0), route.bearerType());
    }

    if (route == AuthenticationRoutePolicy.Route.LOGIN_USER) {
      throw new CredentialAuthenticationException(HttpStatus.UNAUTHORIZED, LOGIN_TOKEN_INVALID);
    }
    throw new BadCredentialsException(UNAUTHORIZED.stringCode());
  }

  public void rejectConflicts(HttpServletRequest request) {
    List<String> apiKeyHeaders = headerValues(request, API_KEY_HEADER);
    List<String> authorizationHeaders = headerValues(request, HttpHeaders.AUTHORIZATION);
    if (hasDuplicates(apiKeyHeaders)
        || hasDuplicates(authorizationHeaders)
        || (!apiKeyHeaders.isEmpty() && !authorizationHeaders.isEmpty())) {
      throw new CredentialAuthenticationException(HttpStatus.BAD_REQUEST, AMBIGUOUS_CREDENTIALS);
    }
  }

  private RequestCredential bearerCredential(String header, AuthenticationType bearerType) {
    if (header == null) {
      throw invalidBearer(bearerType);
    }
    int separator = header.indexOf(' ');
    if (separator <= 0
        || !BEARER_SCHEME.equalsIgnoreCase(header.substring(0, separator))
        || header.substring(separator + 1).isBlank()
        || header.substring(separator + 1).indexOf(' ') >= 0) {
      throw invalidBearer(bearerType);
    }
    return new RequestCredential(bearerType, header.substring(separator + 1));
  }

  private CredentialAuthenticationException invalidBearer(AuthenticationType bearerType) {
    if (bearerType == AuthenticationType.LOGIN_JWT) {
      return new CredentialAuthenticationException(HttpStatus.UNAUTHORIZED, LOGIN_TOKEN_INVALID);
    }
    return new CredentialAuthenticationException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED);
  }

  private List<String> headerValues(HttpServletRequest request, String name) {
    Enumeration<String> values = request.getHeaders(name);
    List<String> result = new ArrayList<>();
    while (values != null && values.hasMoreElements()) {
      result.add(values.nextElement());
    }
    return result;
  }

  private boolean hasDuplicates(List<String> values) {
    return values.size() > 1
        || (values.size() == 1 && values.get(0) != null && values.get(0).contains(","));
  }
}
