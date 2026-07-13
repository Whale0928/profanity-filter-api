package app.security.filter;

import app.security.authentication.CredentialAuthenticationException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** 인증 제외 경로를 포함한 모든 요청에서 다중 credential 충돌만 선행 검사합니다. */
@RequiredArgsConstructor
public class CredentialConflictFilter extends OncePerRequestFilter {
  private final RequestCredentialResolver credentialResolver;
  private final CustomAuthenticationEntryPoint authenticationEntryPoint;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    try {
      credentialResolver.rejectConflicts(request);
    } catch (CredentialAuthenticationException exception) {
      SecurityContextHolder.clearContext();
      request.setAttribute("exception", exception);
      authenticationEntryPoint.commence(request, response, exception);
      return;
    }
    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    return request.getDispatcherType() == DispatcherType.ERROR
        || HttpMethod.OPTIONS.matches(request.getMethod());
  }
}
