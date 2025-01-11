package app.security.filter;

import app.security.authentication.AuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
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
            @NonNull FilterChain filterChain) {
        try {
            Authentication authentication = authenticationService.getAuthentication(request);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (Exception exp) {
            request.setAttribute("exception", exp);
            customAuthenticationEntryPoint
                    .commence(request, response, new AuthenticationException(exp.getMessage()) {
                            }
                    );
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        log.info("Request URI: {}", path);

        // 정적 리소스 체크
        if (PathRequest.toStaticResources().atCommonLocations().matches(request)) {
            return true;
        }

        if (path.equals("/") || path.equals("/index.html")) {
            return true;
        }

        // 제외 경로 정확한 체크
        return ExcludePath.getPaths()
                .stream()
                .anyMatch(excludePath -> excludePath.isMatch(path, method));
    }
}
