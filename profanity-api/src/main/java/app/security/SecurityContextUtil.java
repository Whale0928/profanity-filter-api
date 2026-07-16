package app.security;

import app.domain.client.PermissionsType;
import app.security.authentication.ApiKeyPrincipal;
import app.security.authentication.AuthenticationType;
import app.security.authentication.LoginUserPrincipal;
import app.security.authentication.ServicePrincipal;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Component
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecurityContextUtil {

  /** SecurityContext에서 서비스 인증 주체를 조회합니다. */
  public static ServicePrincipal getAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof ServicePrincipal principal)) {
      return null;
    }
    return principal;
  }

  /** 현재 인증 타입을 반환합니다. */
  public static AuthenticationType getAuthenticationType() {
    ServicePrincipal principal = getAuthentication();
    return principal == null ? null : principal.authenticationType();
  }

  /** 현재 요청이 API Key로 인증됐는지 확인합니다. */
  public static boolean isApiKeyAuthentication() {
    return getAuthenticationType() == AuthenticationType.API_KEY;
  }

  /** 현재 요청이 로그인 JWT로 인증됐는지 확인합니다. */
  public static boolean isLoginJwtAuthentication() {
    return getAuthenticationType() == AuthenticationType.LOGIN_JWT;
  }

  /** 현재 접근 주체가 인증된 클라이언트인지 확인합니다. (익명 사용자, 차단 또는 폐기된 상태가 아님) */
  public static boolean isVerifiedClient() {
    if (!isApiKeyAuthentication()) {
      return false;
    }
    return !hasPermission(PermissionsType.BLOCK) && !hasPermission(PermissionsType.DISCARD);
  }

  /** 현재 접근 주체가 차단 상태인지 확인합니다. */
  public static boolean isBlockedClient() {
    return hasPermission(PermissionsType.BLOCK);
  }

  /** 현재 접근 주체가 폐기 상태인지 확인합니다. */
  public static boolean isDiscardedClient() {
    return hasPermission(PermissionsType.DISCARD);
  }

  /** 현재 접근 주체가 특정 권한을 가지고 있는지 확인하는 유틸리티 메소드입니다. */
  private static boolean hasPermission(PermissionsType permissionType) {
    ServicePrincipal principal = getAuthentication();
    if (!(principal instanceof ApiKeyPrincipal apiKeyPrincipal)) {
      return false;
    }
    List<String> permissions = apiKeyPrincipal.permissions();
    log.debug("Checking permission {} for current permissions: {}", permissionType, permissions);
    return permissions.contains(permissionType.getValue());
  }

  /** 현재 사용자가 인증되었는지 확인합니다. */
  public static boolean isAuthenticated() {
    return getAuthentication() != null;
  }

  /**
   * 현재 API Key 클라이언트의 ID를 반환합니다.
   *
   * @throws IllegalStateException 인증되지 않은 경우
   */
  public static UUID getCurrentApiClientId() {
    return getApiKeyPrincipalWithCheck().id();
  }

  /** 기존 호출부 호환용 별칭입니다. 새 코드에서는 getCurrentApiClientId를 사용합니다. */
  @Deprecated(forRemoval = false)
  public static UUID getCurrentUserId() {
    return getCurrentApiClientId();
  }

  /** 현재 로그인 사용자의 ID를 반환합니다. */
  public static UUID getCurrentLoginUserId() {
    return getLoginUserPrincipalWithCheck().id();
  }

  /**
   * 현재 인증된 사용자의 API 키를 반환합니다.
   *
   * @throws IllegalStateException 인증되지 않은 경우
   */
  public static String getCurrentApikey() {
    return getCurrentApiKey();
  }

  /** 현재 API Key 원문을 반환합니다. */
  public static String getCurrentApiKey() {
    getApiKeyPrincipalWithCheck();
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication.getCredentials() instanceof String apiKey) || apiKey.isBlank()) {
      throw new IllegalStateException("API key credential is unavailable");
    }
    return apiKey;
  }

  /** 현재 API Key의 저장용 SHA-256 해시를 반환합니다. */
  public static String getCurrentApiKeyHash() {
    return getApiKeyPrincipalWithCheck().keyHash();
  }

  /**
   * 현재 인증된 사용자의 이메일을 반환합니다.
   *
   * @throws IllegalStateException 인증되지 않은 경우
   */
  public static String getCurrentUserEmail() {
    return getAuthenticationWithCheck().email();
  }

  /**
   * 현재 인증된 사용자의 권한 목록을 반환합니다.
   *
   * @throws IllegalStateException 인증되지 않은 경우
   */
  public static List<String> getCurrentUserPermissions() {
    return getApiKeyPrincipalWithCheck().permissions();
  }

  private static ServicePrincipal getAuthenticationWithCheck() {
    ServicePrincipal principal = getAuthentication();
    if (principal == null) {
      throw new IllegalStateException("Not authenticated");
    }
    return principal;
  }

  private static ApiKeyPrincipal getApiKeyPrincipalWithCheck() {
    ServicePrincipal principal = getAuthenticationWithCheck();
    if (!(principal instanceof ApiKeyPrincipal apiKeyPrincipal)) {
      throw new IllegalStateException("API key authentication is required");
    }
    return apiKeyPrincipal;
  }

  private static LoginUserPrincipal getLoginUserPrincipalWithCheck() {
    ServicePrincipal principal = getAuthenticationWithCheck();
    if (!(principal instanceof LoginUserPrincipal loginUserPrincipal)) {
      throw new IllegalStateException("Login JWT authentication is required");
    }
    return loginUserPrincipal;
  }
}
