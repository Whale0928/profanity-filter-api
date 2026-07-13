package app.security.jwt;

import static app.core.data.response.constant.StatusCode.LOGIN_TOKEN_EXPIRED;
import static app.core.data.response.constant.StatusCode.LOGIN_TOKEN_INVALID;
import static app.core.data.response.constant.StatusCode.USER_INACTIVE;

import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import app.security.authentication.AuthenticationType;
import app.security.authentication.CredentialAuthenticationException;
import app.security.authentication.CustomAuthentication;
import app.security.authentication.LoginUserPrincipal;
import app.security.authentication.RequestAuthenticator;
import app.security.filter.RequestCredential;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

@Component
public class LoginJwtService implements RequestAuthenticator {
  private static final String TOKEN_USE_CLAIM = "token_use";
  private static final String AUTH_TYPE_CLAIM = "auth_type";
  private static final String ACCESS_TOKEN_USE = "access";
  private static final String USER_AUTHORITY = "ROLE_USER";
  private static final String RS256 = "RS256";

  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final JwtSecurityProperties properties;
  private final UserAccountRepository userAccountRepository;
  private final Clock clock;

  @Autowired
  public LoginJwtService(
      JwtEncoder jwtEncoder,
      JwtDecoder jwtDecoder,
      JwtSecurityProperties properties,
      UserAccountRepository userAccountRepository) {
    this(jwtEncoder, jwtDecoder, properties, userAccountRepository, Clock.systemUTC());
  }

  LoginJwtService(
      JwtEncoder jwtEncoder,
      JwtDecoder jwtDecoder,
      JwtSecurityProperties properties,
      UserAccountRepository userAccountRepository,
      Clock clock) {
    this.jwtEncoder = jwtEncoder;
    this.jwtDecoder = jwtDecoder;
    this.properties = properties;
    this.userAccountRepository = userAccountRepository;
    this.clock = clock;
  }

  public IssuedAccessToken issue(UserAccount userAccount) {
    requireActive(userAccount);
    Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
    Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .audience(List.of(properties.audience()))
            .subject(userAccount.getId().toString())
            .issuedAt(issuedAt)
            .notBefore(issuedAt)
            .expiresAt(expiresAt)
            .id(UUID.randomUUID().toString())
            .claim(TOKEN_USE_CLAIM, ACCESS_TOKEN_USE)
            .claim(AUTH_TYPE_CLAIM, AuthenticationType.LOGIN_JWT.name())
            .build();
    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedAccessToken(token, expiresAt);
  }

  public Authentication authenticate(String token) {
    Jwt jwt = decode(token);
    UUID userId;
    try {
      userId = validate(jwt);
    } catch (CredentialAuthenticationException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new CredentialAuthenticationException(
          HttpStatus.UNAUTHORIZED, LOGIN_TOKEN_INVALID, exception);
    }
    UserAccount userAccount =
        userAccountRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new CredentialAuthenticationException(
                        HttpStatus.UNAUTHORIZED, LOGIN_TOKEN_INVALID));
    requireActive(userAccount);

    LoginUserPrincipal principal =
        new LoginUserPrincipal(userAccount.getId(), userAccount.getPrimaryEmail());
    return new CustomAuthentication(
        AuthenticationType.LOGIN_JWT,
        null,
        List.of(
            new SimpleGrantedAuthority(AuthenticationType.LOGIN_JWT.authority()),
            new SimpleGrantedAuthority(USER_AUTHORITY)),
        principal);
  }

  @Override
  public AuthenticationType supports() {
    return AuthenticationType.LOGIN_JWT;
  }

  @Override
  public Authentication authenticate(RequestCredential credential) {
    if (credential.type() != AuthenticationType.LOGIN_JWT) {
      throw invalidToken();
    }
    return authenticate(credential.value());
  }

  private Jwt decode(String token) {
    if (token == null || token.isBlank()) {
      throw invalidToken();
    }
    try {
      return jwtDecoder.decode(token);
    } catch (JwtValidationException exception) {
      if (isExpired(exception.getErrors())) {
        throw new CredentialAuthenticationException(
            HttpStatus.UNAUTHORIZED, LOGIN_TOKEN_EXPIRED, exception);
      }
      throw new CredentialAuthenticationException(
          HttpStatus.UNAUTHORIZED, LOGIN_TOKEN_INVALID, exception);
    } catch (JwtException | IllegalArgumentException exception) {
      throw new CredentialAuthenticationException(
          HttpStatus.UNAUTHORIZED, LOGIN_TOKEN_INVALID, exception);
    }
  }

  private UUID validate(Jwt jwt) {
    Instant now = clock.instant();
    Instant issuedAt = jwt.getIssuedAt();
    Instant notBefore = jwt.getNotBefore();
    Instant expiresAt = jwt.getExpiresAt();

    if (!RS256.equals(Objects.toString(jwt.getHeaders().get("alg"), null))
        || !properties.issuer().equals(jwt.getClaimAsString(JwtClaimNames.ISS))
        || jwt.getAudience() == null
        || !jwt.getAudience().contains(properties.audience())
        || !ACCESS_TOKEN_USE.equals(jwt.getClaimAsString(TOKEN_USE_CLAIM))
        || !AuthenticationType.LOGIN_JWT.name().equals(jwt.getClaimAsString(AUTH_TYPE_CLAIM))
        || issuedAt == null
        || notBefore == null
        || expiresAt == null
        || jwt.getId() == null
        || jwt.getId().isBlank()) {
      throw invalidToken();
    }
    if (expiresAt.plus(properties.clockSkew()).isBefore(now)
        || expiresAt.plus(properties.clockSkew()).equals(now)) {
      throw new CredentialAuthenticationException(HttpStatus.UNAUTHORIZED, LOGIN_TOKEN_EXPIRED);
    }
    if (issuedAt.minus(properties.clockSkew()).isAfter(now)
        || notBefore.minus(properties.clockSkew()).isAfter(now)
        || issuedAt.isAfter(expiresAt)
        || notBefore.isAfter(expiresAt)) {
      throw invalidToken();
    }

    try {
      return UUID.fromString(jwt.getSubject());
    } catch (RuntimeException exception) {
      throw new CredentialAuthenticationException(
          HttpStatus.UNAUTHORIZED, LOGIN_TOKEN_INVALID, exception);
    }
  }

  private void requireActive(UserAccount userAccount) {
    if (userAccount == null || !userAccount.isActive()) {
      throw new CredentialAuthenticationException(HttpStatus.FORBIDDEN, USER_INACTIVE);
    }
  }

  private boolean isExpired(Iterable<OAuth2Error> errors) {
    for (OAuth2Error error : errors) {
      String description = error.getDescription();
      if (description != null && description.toLowerCase(Locale.ROOT).contains("expired")) {
        return true;
      }
    }
    return false;
  }

  private CredentialAuthenticationException invalidToken() {
    return new CredentialAuthenticationException(HttpStatus.UNAUTHORIZED, LOGIN_TOKEN_INVALID);
  }
}
