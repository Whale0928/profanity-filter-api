package app.security.oauth2;

import java.util.Locale;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** Google OIDC 사용자의 검증된 이메일을 로그인 필수 조건으로 확인합니다. */
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
  private static final String GOOGLE_REGISTRATION_ID = "google";
  private static final String INVALID_USER_INFO_RESPONSE = "invalid_user_info_response";

  private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

  public GoogleOidcUserService() {
    this(new OidcUserService());
  }

  GoogleOidcUserService(OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
    this.delegate = delegate;
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser user = delegate.loadUser(userRequest);
    if (GOOGLE_REGISTRATION_ID.equals(userRequest.getClientRegistration().getRegistrationId())) {
      requireAuthoritativeVerifiedEmail(user);
    }
    return user;
  }

  private void requireAuthoritativeVerifiedEmail(OidcUser user) {
    String email = user.getAttribute("email");
    Boolean emailVerified = user.getAttribute("email_verified");
    String hostedDomain = user.getIdToken().getClaimAsString("hd");
    boolean emailAuthoritative =
        email != null
            && (email.toLowerCase(Locale.ROOT).endsWith("@gmail.com")
                || hostedDomain != null && !hostedDomain.isBlank());
    if (email == null
        || email.isBlank()
        || !Boolean.TRUE.equals(emailVerified)
        || !emailAuthoritative) {
      OAuth2Error error =
          new OAuth2Error(
              INVALID_USER_INFO_RESPONSE,
              "Google authoritative verified email is unavailable",
              null);
      throw new OAuth2AuthenticationException(error, error.toString());
    }
  }
}
