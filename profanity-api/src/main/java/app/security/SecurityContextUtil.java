package app.security;

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
