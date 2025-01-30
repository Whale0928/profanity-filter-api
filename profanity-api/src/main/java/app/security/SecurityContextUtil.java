package app.security;

import app.domain.client.PermissionsType;
import app.security.authentication.CustomPrincipal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Getter
@Component
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecurityContextUtil {
    public static CustomPrincipal getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return CustomPrincipal.anonymous();
        }
        return CustomPrincipal.of(authentication.getPrincipal());
    }

    /**
     * 현재 접근 주체가 인증된 클라이언트인지 확인합니다 (익명 사용자, 차단 또는 폐기된 상태가 아님).
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
        return permissions != null && permissions.contains(permissionType.getValue());
    }

    public static boolean isAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    public static UUID getCurrentUserId() {
        return getAuthentication().id();
    }

    public static String getCurrentApikey() {
        return getAuthentication().apiKey();
    }

    public static String getCurrentUserEmail() {
        return getAuthentication().email();
    }

    public static List<String> getCurrentUserPermissions() {
        return getAuthentication().permissions();
    }
}
