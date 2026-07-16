package app.security.filter;

import app.security.authentication.AuthenticationService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {

  private final AuthenticationService authenticationService;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication;
    try {
      authentication = authenticationService.getAuthentication(request);
    } catch (AuthenticationException exp) {
      SecurityContextHolder.clearContext();
      request.setAttribute("exception", exp);
      customAuthenticationEntryPoint.commence(request, response, exp);
      return;
    }

    var securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = pathWithinApplication(request);
    String method = request.getMethod();
    log.debug("ip : {}, path : {}, method : {}", request.getRemoteAddr(), path, method);
    if (request.getDispatcherType() == DispatcherType.ERROR || HttpMethod.OPTIONS.matches(method)) {
      return true;
    }
    if (isStaticResource(path)) {
      return true;
    }

    if (path.equals("/") || path.equals("/index.html")) {
      return true;
    }
    return ExcludePath.getPaths().stream()
        .anyMatch(excludePath -> excludePath.isMatch(path, method));
  }

  private boolean isStaticResource(String path) {
    return path.startsWith("/css/")
        || path.startsWith("/js/")
        || path.startsWith("/images/")
        || path.startsWith("/webjars/")
        || path.matches("/favicon\\.[^/]+")
        || path.matches("/[^/]+/icon-[^/]+");
  }

  private String pathWithinApplication(HttpServletRequest request) {
    String path = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
      return path.substring(contextPath.length());
    }
    return path;
  }
}
