package app.presentation;

import app.application.auth.LoginAuthService;
import app.application.auth.LoginAuthService.LoginTokenBundle;
import app.core.data.response.ApiResponse;
import app.dto.request.AuthCodeExchangeRequest;
import app.dto.response.CsrfTokenResponse;
import app.dto.response.LoginTokenResponse;
import app.dto.response.LoginUserResponse;
import app.openapi.AuthOpenApi;
import app.security.SecurityContextUtil;
import app.security.login.LoginFlowException;
import app.security.login.LoginRefreshCookieWriter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@AuthOpenApi.ApiTag
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {
  private final LoginAuthService loginAuthService;
  private final LoginRefreshCookieWriter refreshCookieWriter;

  @AuthOpenApi.Exchange
  @PostMapping(value = "/exchange", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<LoginTokenResponse>> exchange(
      @Valid @RequestBody AuthCodeExchangeRequest request, HttpServletResponse response) {
    LoginTokenBundle bundle = loginAuthService.exchange(request.code());
    refreshCookieWriter.write(response, bundle.refreshToken(), bundle.refreshMaxAge());
    return noStore(tokenResponse(bundle));
  }

  @AuthOpenApi.Csrf
  @GetMapping("/csrf")
  public ResponseEntity<ApiResponse<CsrfTokenResponse>> csrf(HttpServletRequest request) {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    if (csrfToken == null) {
      csrfToken = (CsrfToken) request.getAttribute("_csrf");
    }
    if (csrfToken == null) {
      throw new IllegalStateException("CSRF token is unavailable");
    }
    return noStore(new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken()));
  }

  @AuthOpenApi.Refresh
  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<LoginTokenResponse>> refresh(
      HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = refreshCookie(request);
    try {
      LoginTokenBundle bundle = loginAuthService.refresh(refreshToken);
      refreshCookieWriter.write(response, bundle.refreshToken(), bundle.refreshMaxAge());
      return noStore(tokenResponse(bundle));
    } catch (LoginFlowException exception) {
      if (exception.isExpireRefreshCookie()) {
        refreshCookieWriter.expire(response);
      }
      throw exception;
    }
  }

  @AuthOpenApi.Me
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<LoginUserResponse>> me() {
    UUID userId = SecurityContextUtil.getCurrentLoginUserId();
    return noStore(userResponse(loginAuthService.currentUser(userId)));
  }

  private String refreshCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return "";
    }
    return Arrays.stream(cookies)
        .filter(cookie -> refreshCookieWriter.cookieName().equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse("");
  }

  private LoginTokenResponse tokenResponse(LoginTokenBundle bundle) {
    return new LoginTokenResponse(
        bundle.accessToken(),
        "Bearer",
        bundle.accessExpiresIn(),
        userResponse(bundle.userAccount()));
  }

  private LoginUserResponse userResponse(app.domain.user.UserAccount userAccount) {
    return new LoginUserResponse(
        userAccount.getId(),
        userAccount.getDisplayName(),
        userAccount.getPrimaryEmail(),
        userAccount.getAvatarUrl());
  }

  private <T> ResponseEntity<ApiResponse<T>> noStore(T data) {
    ResponseEntity<ApiResponse<T>> response = ApiResponse.ok(data);
    return ResponseEntity.status(response.getStatusCode())
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(response.getBody());
  }
}
