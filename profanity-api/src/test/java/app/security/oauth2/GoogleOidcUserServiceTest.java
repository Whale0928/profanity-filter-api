package app.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class GoogleOidcUserServiceTest {

  @Test
  @DisplayName("Google의 검증된 Gmail 주소가 있으면 OIDC 사용자를 반환한다")
  void loadUser_whenVerifiedEmailIsGmail_returnsOidcUser() {
    OidcUser oidcUser = oidcUser("hgkim@gmail.com", true, null);
    GoogleOidcUserService service = new GoogleOidcUserService(ignored -> oidcUser);

    OidcUser loaded = service.loadUser(userRequest(oidcUser.getIdToken()));

    assertThat(loaded).isSameAs(oidcUser);
    assertThat(loaded.getEmail()).isEqualTo("hgkim@gmail.com");
  }

  @Test
  @DisplayName("Google 이메일이 검증되지 않으면 로그인을 거부한다")
  void loadUser_whenEmailIsUnverified_throwsOAuth2AuthenticationException() {
    OidcUser oidcUser = oidcUser("hgkim@gmail.com", false, null);
    GoogleOidcUserService service = new GoogleOidcUserService(ignored -> oidcUser);

    assertThatThrownBy(() -> service.loadUser(userRequest(oidcUser.getIdToken())))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .hasMessageContaining("invalid_user_info_response");
  }

  @Test
  @DisplayName("Google ID token에 hosted domain이 있으면 외부 이메일 로그인을 허용한다")
  void loadUser_whenSignedHostedDomainExists_returnsOidcUser() {
    OidcUser oidcUser = oidcUser("hgkim@company.example", true, "company.example");
    GoogleOidcUserService service = new GoogleOidcUserService(ignored -> oidcUser);

    OidcUser loaded = service.loadUser(userRequest(oidcUser.getIdToken()));

    assertThat(loaded).isSameAs(oidcUser);
  }

  @Test
  @DisplayName("Google 외부 이메일에 signed hosted domain이 없으면 로그인을 거부한다")
  void loadUser_whenExternalEmailHasNoSignedHostedDomain_throwsOAuth2AuthenticationException() {
    OidcUser oidcUser = oidcUser("hgkim@example.com", true, null);
    GoogleOidcUserService service = new GoogleOidcUserService(ignored -> oidcUser);

    assertThatThrownBy(() -> service.loadUser(userRequest(oidcUser.getIdToken())))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .hasMessageContaining("invalid_user_info_response");
  }

  @Test
  @DisplayName("hosted domain이 UserInfo에만 있으면 signed claim으로 신뢰하지 않는다")
  void loadUser_whenHostedDomainExistsOnlyInUserInfo_throwsOAuth2AuthenticationException() {
    OidcUser idTokenUser = oidcUser("hgkim@example.com", true, null);
    OidcUserInfo userInfo =
        new OidcUserInfo(
            Map.of(
                "sub", "google-user-123",
                "email", "hgkim@example.com",
                "email_verified", true,
                "hd", "company.example"));
    OidcUser mergedUser =
        new DefaultOidcUser(
            List.of(new SimpleGrantedAuthority("OIDC_USER")),
            idTokenUser.getIdToken(),
            userInfo,
            "sub");
    GoogleOidcUserService service = new GoogleOidcUserService(ignored -> mergedUser);

    assertThatThrownBy(() -> service.loadUser(userRequest(mergedUser.getIdToken())))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .hasMessageContaining("invalid_user_info_response");
  }

  private OidcUser oidcUser(String email, boolean emailVerified, String hostedDomain) {
    Instant issuedAt = Instant.now();
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("sub", "google-user-123");
    claims.put("email", email);
    claims.put("email_verified", emailVerified);
    claims.put("name", "HG Kim");
    if (hostedDomain != null) {
      claims.put("hd", hostedDomain);
    }
    OidcIdToken idToken = new OidcIdToken("id-token", issuedAt, issuedAt.plusSeconds(300), claims);
    return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken, "sub");
  }

  private OidcUserRequest userRequest(OidcIdToken idToken) {
    ClientRegistration registration =
        ClientRegistration.withRegistrationId("google")
            .clientId("client-id")
            .clientSecret("client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/login/oauth2/code/google")
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.example.test/oauth2/authorize")
            .tokenUri("https://accounts.example.test/oauth2/token")
            .jwkSetUri("https://accounts.example.test/oauth2/certs")
            .userInfoUri("https://accounts.example.test/userinfo")
            .userNameAttributeName("sub")
            .clientName("Google")
            .build();
    Instant issuedAt = Instant.now();
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "google-access-token",
            issuedAt,
            issuedAt.plusSeconds(300),
            Set.of("openid", "profile", "email"));
    return new OidcUserRequest(registration, accessToken, idToken);
  }
}
