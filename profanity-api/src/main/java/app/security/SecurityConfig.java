package app.security;

import app.security.authentication.AuthenticationService;
import app.security.filter.CustomAuthenticationEntryPoint;
import app.security.filter.CustomAuthenticationFilter;
import app.security.oauth2.CookieOAuth2AuthorizationRequestRepository;
import app.security.oauth2.OAuth2LoginFailureHandler;
import app.security.oauth2.OAuth2LoginSuccessHandler;
import app.security.oauth2.OAuth2SecuritySupportConfig;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@Import(OAuth2SecuritySupportConfig.class)
@RequiredArgsConstructor
public class SecurityConfig {

  private final AuthenticationService authenticationService;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final CookieOAuth2AuthorizationRequestRepository
      cookieOAuth2AuthorizationRequestRepository;
  private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
  private final OAuth2LoginFailureHandler oauth2LoginFailureHandler;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(
            sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable)
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
            authorizationManagerRequestMatcherRegistry ->
                authorizationManagerRequestMatcherRegistry
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/", "/index.html")
                    .permitAll()
                    .requestMatchers("/sso/**", "/oauth2/**", "/login/oauth2/code/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/clients/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/health")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/ping")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/openapi.json")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/overview.md")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/llms.txt", "/llm.txt")
                    .permitAll()
                    .requestMatchers("/api/v1/clients/send-email")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(
            new CustomAuthenticationFilter(authenticationService, customAuthenticationEntryPoint),
            UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(
            exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(customAuthenticationEntryPoint))
        .build();
  }

  /**
   * Cors 구성 소스 빈 등록
   *
   * @return the cors configuration source
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowCredentials(false); // 변경
    configuration.setAllowedOrigins(List.of("*"));
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public UserDetailsService userDetailsService() {
    return new InMemoryUserDetailsManager();
  }
}
