package app.security;

import app.security.authentication.AuthenticationService;
import app.security.filter.CredentialConflictFilter;
import app.security.filter.CustomAccessDeniedHandler;
import app.security.filter.CustomAuthenticationEntryPoint;
import app.security.filter.CustomAuthenticationFilter;
import app.security.filter.RequestCredentialResolver;
import app.security.login.LoginSessionProperties;
import app.security.oauth2.CookieOAuth2AuthorizationRequestRepository;
import app.security.oauth2.OAuth2LoginFailureHandler;
import app.security.oauth2.OAuth2LoginSuccessHandler;
import app.security.oauth2.OAuth2SecuritySupportConfig;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@Import(OAuth2SecuritySupportConfig.class)
@RequiredArgsConstructor
public class SecurityConfig {
  private static final String AUTH_API_KEY = "AUTH_API_KEY";
  private static final String AUTH_LOGIN_JWT = "AUTH_LOGIN_JWT";

  private final AuthenticationService authenticationService;
  private final RequestCredentialResolver requestCredentialResolver;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;
  private final CookieOAuth2AuthorizationRequestRepository
      cookieOAuth2AuthorizationRequestRepository;
  private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
  private final OAuth2LoginFailureHandler oauth2LoginFailureHandler;
  private final LoginSessionProperties loginSessionProperties;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrfTokenRepository.setCookiePath("/");
    csrfTokenRepository.setCookieCustomizer(
        cookie ->
            cookie
                .secure(loginSessionProperties.refreshCookie().secure())
                .sameSite(loginSessionProperties.refreshCookie().sameSite()));

    RequestMatcher refreshCsrfMatcher =
        request ->
            HttpMethod.POST.matches(request.getMethod())
                && "/api/v1/auth/refresh".equals(pathWithinApplication(request));

    return http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(
            sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(csrfTokenRepository)
                    .requireCsrfProtectionMatcher(refreshCsrfMatcher))
        .headers(
            headersConfigurer ->
                headersConfigurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
        .oauth2Login(
            oauth2 ->
                oauth2
                    .authorizationEndpoint(
                        authorization ->
                            authorization.authorizationRequestRepository(
                                cookieOAuth2AuthorizationRequestRepository))
                    .successHandler(oauth2LoginSuccessHandler)
                    .failureHandler(oauth2LoginFailureHandler))
        .authorizeHttpRequests(
            authorization ->
                authorization
                    .dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/", "/index.html")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/sso/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET, "/oauth2/authorization/**", "/login/oauth2/code/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/health", "/api/v1/ping")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET, "/openapi.json", "/overview.md", "/llms.txt", "/llm.txt")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/clients/register")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET, "/api/v1/clients/send-email", "/api/v1/auth/csrf")
                    .permitAll()
                    .requestMatchers(HttpMethod.PUT, "/api/v1/clients/send-email")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST, "/api/v1/auth/exchange", "/api/v1/auth/refresh")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/auth/me", "/api/v1/dashboard/**")
                    .hasAuthority(AUTH_LOGIN_JWT)
                    .requestMatchers(
                        "/api/v1/filter/**",
                        "/api/v1/clients",
                        "/api/v1/clients/update",
                        "/api/v1/clients/reissue",
                        "/api/v1/word/**",
                        "/api/v1/sync")
                    .hasAuthority(AUTH_API_KEY)
                    .anyRequest()
                    .denyAll())
        .addFilterBefore(
            new CustomAuthenticationFilter(authenticationService, customAuthenticationEntryPoint),
            UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(
            new CredentialConflictFilter(requestCredentialResolver, customAuthenticationEntryPoint),
            CustomAuthenticationFilter.class)
        .exceptionHandling(
            exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler))
        .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration loginConfiguration = new CorsConfiguration();
    loginConfiguration.setAllowCredentials(true);
    loginConfiguration.setAllowedOrigins(loginSessionProperties.allowedOrigins());
    loginConfiguration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    loginConfiguration.setAllowedHeaders(
        List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "X-API-KEY", "X-XSRF-TOKEN"));

    CorsConfiguration legacyApiConfiguration = new CorsConfiguration();
    legacyApiConfiguration.setAllowCredentials(false);
    legacyApiConfiguration.setAllowedOrigins(List.of("*"));
    legacyApiConfiguration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    legacyApiConfiguration.setAllowedHeaders(List.of("*"));

    return request ->
        isLoginBrowserPath(pathWithinApplication(request))
            ? loginConfiguration
            : legacyApiConfiguration;
  }

  private static boolean isLoginBrowserPath(String path) {
    return path.startsWith("/api/v1/auth/")
        || path.equals("/api/v1/dashboard")
        || path.startsWith("/api/v1/dashboard/");
  }

  private static String pathWithinApplication(HttpServletRequest request) {
    String path = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
      return path.substring(contextPath.length());
    }
    return path;
  }

  @Bean
  public UserDetailsService userDetailsService() {
    return new InMemoryUserDetailsManager();
  }
}
