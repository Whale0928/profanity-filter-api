package app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.domain.client.PermissionsType;
import app.security.authentication.ApiKeyPrincipal;
import app.security.authentication.AuthenticationType;
import app.security.authentication.CustomAuthentication;
import app.security.authentication.LoginUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextUtilTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("API Key 인증에서 클라이언트 ID와 credential을 명시적으로 조회한다")
  void apiKeyAuthentication_whenRead_returnsApiClientValues() {
    UUID clientId = UUID.randomUUID();
    ApiKeyPrincipal principal =
        new ApiKeyPrincipal(
            clientId, "client@example.com", "test", List.of("READ"), "2026-07-11", "key-hash");
    setAuthentication(
        new CustomAuthentication(
            AuthenticationType.API_KEY,
            "secret-api-key",
            List.of(
                new SimpleGrantedAuthority("AUTH_API_KEY"),
                new SimpleGrantedAuthority("ROLE_READ")),
            principal));

    assertThat(SecurityContextUtil.isApiKeyAuthentication()).isTrue();
    assertThat(SecurityContextUtil.isLoginJwtAuthentication()).isFalse();
    assertThat(SecurityContextUtil.getCurrentApiClientId()).isEqualTo(clientId);
    assertThat(SecurityContextUtil.getCurrentUserId()).isEqualTo(clientId);
    assertThat(SecurityContextUtil.getCurrentApiKey()).isEqualTo("secret-api-key");
    assertThat(SecurityContextUtil.getCurrentApiKeyHash()).isEqualTo("key-hash");
    assertThat(SecurityContextUtil.getCurrentUserPermissions()).containsExactly("READ");
    assertThat(SecurityContextUtil.isVerifiedClient()).isTrue();
  }

  @Test
  @DisplayName("LOGIN_JWT 인증은 API 클라이언트 검증을 통과하지 않는다")
  void loginJwtAuthentication_whenClientCheck_returnsFalse() {
    UUID userId = UUID.randomUUID();
    setAuthentication(
        new CustomAuthentication(
            AuthenticationType.LOGIN_JWT,
            null,
            List.of(
                new SimpleGrantedAuthority("AUTH_LOGIN_JWT"),
                new SimpleGrantedAuthority("ROLE_USER")),
            new LoginUserPrincipal(userId, "user@example.com")));

    assertThat(SecurityContextUtil.isLoginJwtAuthentication()).isTrue();
    assertThat(SecurityContextUtil.getCurrentLoginUserId()).isEqualTo(userId);
    assertThat(SecurityContextUtil.isVerifiedClient()).isFalse();
    assertThatThrownBy(SecurityContextUtil::getCurrentApiClientId)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("API key authentication");
    assertThatThrownBy(SecurityContextUtil::getCurrentApiKey)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("API key authentication");
  }

  @Test
  @DisplayName("차단 권한은 API Key 주체에서만 평가한다")
  void apiKeyAuthentication_withBlockedPermission_isBlocked() {
    ApiKeyPrincipal principal =
        new ApiKeyPrincipal(
            UUID.randomUUID(),
            "blocked@example.com",
            "test",
            List.of(PermissionsType.BLOCK.getValue()),
            "2026-07-11",
            "key-hash");
    setAuthentication(
        new CustomAuthentication(AuthenticationType.API_KEY, "redacted", List.of(), principal));

    assertThat(SecurityContextUtil.isBlockedClient()).isTrue();
    assertThat(SecurityContextUtil.isVerifiedClient()).isFalse();
  }

  @Test
  @DisplayName("인증 객체의 문자열에는 credential 원문이 포함되지 않는다")
  void authentication_toString_redactsCredential() {
    CustomAuthentication authentication =
        new CustomAuthentication(
            AuthenticationType.API_KEY,
            "must-not-leak",
            List.of(),
            new ApiKeyPrincipal(
                UUID.randomUUID(), "client@example.com", "test", List.of(), "now", "key-hash"));

    assertThat(authentication.toString()).doesNotContain("must-not-leak");
  }

  private void setAuthentication(CustomAuthentication authentication) {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }
}
