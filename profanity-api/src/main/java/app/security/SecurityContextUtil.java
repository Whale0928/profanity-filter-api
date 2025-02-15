package app.security;

import app.domain.client.PermissionsType;
import app.security.authentication.CustomPrincipal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Getter
@Component
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecurityContextUtil {

    /**
     * SecurityContext에서 CustomPrincipal을 조회합니다.
     * authentication이 null인 경우 anonymous 권한을 가진 Principal을 반환합니다.
     */
    public static CustomPrincipal getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return CustomPrincipal.anonymous();
        }
        return CustomPrincipal.of(authentication.getPrincipal());
    }

    /**
     * 현재 접근 주체가 인증된 클라이언트인지 확인합니다.
     * (익명 사용자, 차단 또는 폐기된 상태가 아님)
     */
    public static boolean isVerifiedClient() {
        CustomPrincipal principal = getAuthentication();
        if (principal == null || "anonymous".equals(principal.issuerInfo())) {
            return false;
        }
        return !hasPermission(PermissionsType.BLOCK) && !hasPermission(PermissionsType.DISCARD);
    }

    /**
     * 현재 접근 주체가 차단 상태인지 확인합니다.
     */
    public static boolean isBlockedClient() {
        return hasPermission(PermissionsType.BLOCK);
    }

    /**
     * 현재 접근 주체가 폐기 상태인지 확인합니다.
     */
    public static boolean isDiscardedClient() {
        return hasPermission(PermissionsType.DISCARD);
    }

    /**
     * 현재 접근 주체가 특정 권한을 가지고 있는지 확인하는 유틸리티 메소드입니다.
     */
    private static boolean hasPermission(PermissionsType permissionType) {
        CustomPrincipal principal = getAuthentication();
        if (principal == null) {
            return false;
        }
        List<String> permissions = principal.permissions();
        log.debug("Checking permission {} for current permissions: {}", permissionType, permissions);
        return permissions != null && permissions.contains(permissionType.getValue());
    }

    /**
     * 현재 사용자가 인증되었는지 확인합니다.
     */
    public static boolean isAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     *
     * @throws IllegalStateException 인증되지 않은 경우
     */
    public static UUID getCurrentUserId() {
        return getAuthenticationWithCheck().id();
    }

    /**
     * 현재 인증된 사용자의 API 키를 반환합니다.
     *
     * @throws IllegalStateException 인증되지 않은 경우
     */
    public static String getCurrentApikey() {
        return getAuthenticationWithCheck().apiKey();
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
        return getAuthenticationWithCheck().permissions();
    }

    /**
     * 인증 상태를 체크하고 CustomPrincipal을 반환합니다.
     *
     * @throws IllegalStateException 인증되지 않은 경우
     */
    private static CustomPrincipal getAuthenticationWithCheck() {
        CustomPrincipal principal = getAuthentication();
        if (principal == null || "anonymous".equals(principal.issuerInfo())) {
            throw new IllegalStateException("Not authenticated");
        }
        return principal;
    }
}
