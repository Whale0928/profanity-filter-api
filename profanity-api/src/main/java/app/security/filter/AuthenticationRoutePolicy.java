package app.security.filter;

import app.security.authentication.AuthenticationType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** 경로가 Bearer token을 어떤 인증 타입으로 해석할지 결정합니다. */
@Component
public class AuthenticationRoutePolicy {
  private static final String AUTH_ME_PATH = "/api/v1/auth/me";
  private static final String DASHBOARD_ROOT = "/api/v1/dashboard";

  public Route route(HttpServletRequest request) {
    String path = pathWithinApplication(request);
    if (AUTH_ME_PATH.equals(path)
        || DASHBOARD_ROOT.equals(path)
        || path.startsWith(DASHBOARD_ROOT + "/")) {
      return Route.LOGIN_USER;
    }
    return Route.EXTERNAL_API;
  }

  private String pathWithinApplication(HttpServletRequest request) {
    String path = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
      return path.substring(contextPath.length());
    }
    return path;
  }

  public enum Route {
    LOGIN_USER(AuthenticationType.LOGIN_JWT, false),
    EXTERNAL_API(AuthenticationType.OAUTH2_ACCESS_TOKEN, true);

    private final AuthenticationType bearerType;
    private final boolean apiKeyAllowed;

    Route(AuthenticationType bearerType, boolean apiKeyAllowed) {
      this.bearerType = bearerType;
      this.apiKeyAllowed = apiKeyAllowed;
    }

    public AuthenticationType bearerType() {
      return bearerType;
    }

    public boolean apiKeyAllowed() {
      return apiKeyAllowed;
    }
  }
}
