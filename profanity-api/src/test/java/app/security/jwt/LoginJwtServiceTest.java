package app.security.jwt;

import static app.core.data.response.constant.StatusCode.LOGIN_TOKEN_EXPIRED;
import static app.core.data.response.constant.StatusCode.LOGIN_TOKEN_INVALID;
import static app.core.data.response.constant.StatusCode.USER_INACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import app.security.authentication.AuthenticationType;
import app.security.authentication.CredentialAuthenticationException;
import app.security.authentication.CustomAuthentication;
import app.security.authentication.LoginUserPrincipal;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class LoginJwtServiceTest {
  private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);
  private static final String ISSUER = "https://api.example.test";
  private static final String AUDIENCE = "profanity-dashboard";

  private final InMemoryUserAccountRepository userAccountRepository =
      new InMemoryUserAccountRepository();
  private JwtEncoder jwtEncoder;
  private JwtDecoder jwtDecoder;
  private LoginJwtService loginJwtService;

  @BeforeEach
  void setUp() throws Exception {
    RSAKey rsaKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
    jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    jwtDecoder =
        NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey())
            .signatureAlgorithm(SignatureAlgorithm.RS256)
            .build();
    JwtSecurityProperties properties =
        new JwtSecurityProperties(ISSUER, AUDIENCE, Duration.ofMinutes(15), Duration.ofSeconds(30));
    loginJwtService =
        new LoginJwtService(
            jwtEncoder,
            jwtDecoder,
            properties,
            userAccountRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("활성 사용자에게 필수 claim을 가진 RS256 access token을 발급한다")
  void issue_activeUser_returnsRs256AccessTokenWithRequiredClaims() {
    UserAccount user = activeUser();

    IssuedAccessToken issued = loginJwtService.issue(user);
    Jwt jwt = jwtDecoder.decode(issued.token());

    assertThat(jwt.getHeaders().get("alg")).hasToString("RS256");
    assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
    assertThat(jwt.getAudience()).containsExactly(AUDIENCE);
    assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
    assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
    assertThat(jwt.getNotBefore()).isEqualTo(NOW);
    assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    assertThat(jwt.getId()).isNotBlank();
    assertThat(jwt.getClaimAsString("token_use")).isEqualTo("access");
    assertThat(jwt.getClaimAsString("auth_type")).isEqualTo("LOGIN_JWT");
    assertThat(issued.toString()).doesNotContain(issued.token());
  }

  @Test
  @DisplayName("정상 access token은 최신 사용자 정보로 LOGIN_JWT Security Authentication을 생성한다")
  void authenticate_validAccessToken_returnsLoginJwtAuthentication() {
    UserAccount user = userAccountRepository.save(activeUser());
    String token = loginJwtService.issue(user).token();

    Authentication authentication = loginJwtService.authenticate(token);

    assertThat(authentication).isInstanceOf(CustomAuthentication.class);
    assertThat(((CustomAuthentication) authentication).authenticationType())
        .isEqualTo(AuthenticationType.LOGIN_JWT);
    assertThat(authentication.getPrincipal()).isInstanceOf(LoginUserPrincipal.class);
    assertThat(((LoginUserPrincipal) authentication.getPrincipal()).id()).isEqualTo(user.getId());
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly("AUTH_LOGIN_JWT", "ROLE_USER");
    assertThat(authentication.getCredentials()).isNull();
    assertThat(authentication.toString()).doesNotContain(token);
  }

  @Test
  @DisplayName("서명이 변조된 token은 거부한다")
  void authenticate_tamperedToken_throwsLoginTokenInvalid() {
    UserAccount user = userAccountRepository.save(activeUser());
    String token = loginJwtService.issue(user).token();
    String[] parts = token.split("\\.");
    parts[2] = (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
    String tampered = String.join(".", parts);

    assertInvalidToken(tampered, LOGIN_TOKEN_INVALID.stringCode());
  }

  @Test
  @DisplayName("만료된 access token은 만료 코드로 거부한다")
  void authenticate_expiredToken_throwsLoginTokenExpired() {
    UserAccount user = userAccountRepository.save(activeUser());
    String token =
        encode(
            claims(user.getId())
                .issuedAt(NOW.minus(Duration.ofMinutes(20)))
                .notBefore(NOW.minus(Duration.ofMinutes(20)))
                .expiresAt(NOW.minus(Duration.ofMinutes(5)))
                .build());

    assertInvalidToken(token, LOGIN_TOKEN_EXPIRED.stringCode());
  }

  @Test
  @DisplayName("issuer가 다른 token은 거부한다")
  void authenticate_wrongIssuer_throwsLoginTokenInvalid() {
    UserAccount user = userAccountRepository.save(activeUser());
    String token = encode(claims(user.getId()).issuer("https://other.example.test").build());

    assertInvalidToken(token, LOGIN_TOKEN_INVALID.stringCode());
  }

  @Test
  @DisplayName("audience가 다른 token은 거부한다")
  void authenticate_wrongAudience_throwsLoginTokenInvalid() {
    UserAccount user = userAccountRepository.save(activeUser());
    String token = encode(claims(user.getId()).audience(List.of("other-audience")).build());

    assertInvalidToken(token, LOGIN_TOKEN_INVALID.stringCode());
  }

  @Test
  @DisplayName("refresh 용도의 token을 access 인증에 사용할 수 없다")
  void authenticate_refreshTokenUse_throwsLoginTokenInvalid() {
    UserAccount user = userAccountRepository.save(activeUser());
    String token = encode(claims(user.getId()).claim("token_use", "refresh").build());

    assertInvalidToken(token, LOGIN_TOKEN_INVALID.stringCode());
  }

  @Test
  @DisplayName("LOGIN_JWT가 아닌 auth_type token은 거부한다")
  void authenticate_wrongAuthenticationType_throwsLoginTokenInvalid() {
    UserAccount user = userAccountRepository.save(activeUser());
    String token = encode(claims(user.getId()).claim("auth_type", "API_KEY").build());

    assertInvalidToken(token, LOGIN_TOKEN_INVALID.stringCode());
  }

  @Test
  @DisplayName("jti가 없는 token은 거부한다")
  void authenticate_missingJti_throwsLoginTokenInvalid() {
    UserAccount user = userAccountRepository.save(activeUser());
    String token = encode(claimsWithoutId(user.getId()).build());

    assertInvalidToken(token, LOGIN_TOKEN_INVALID.stringCode());
  }

  @Test
  @DisplayName("허용 clock skew보다 미래인 nbf token은 거부한다")
  void authenticate_notBeforeBeyondClockSkew_throwsLoginTokenInvalid() {
    UserAccount user = userAccountRepository.save(activeUser());
    String token = encode(claims(user.getId()).notBefore(NOW.plusSeconds(31)).build());

    assertInvalidToken(token, LOGIN_TOKEN_INVALID.stringCode());
  }

  @Test
  @DisplayName("token 검증 시 사용자가 비활성 상태면 SecurityContext 인증을 만들지 않는다")
  void authenticate_inactiveUser_throwsUserInactive() {
    UserAccount user = userAccountRepository.save(activeUser());
    String token = loginJwtService.issue(user).token();
    user.disable(NOW.plusSeconds(1));

    assertInvalidToken(token, USER_INACTIVE.stringCode());
  }

  @Test
  @DisplayName("비활성 사용자에게 access token을 발급하지 않는다")
  void issue_inactiveUser_throwsUserInactive() {
    UserAccount user = activeUser();
    user.disable(NOW);

    assertThatThrownBy(() -> loginJwtService.issue(user))
        .isInstanceOf(CredentialAuthenticationException.class)
        .hasMessage(USER_INACTIVE.stringCode());
  }

  private JwtClaimsSet.Builder claims(UUID userId) {
    return claimsWithoutId(userId).id(UUID.randomUUID().toString());
  }

  private JwtClaimsSet.Builder claimsWithoutId(UUID userId) {
    return JwtClaimsSet.builder()
        .issuer(ISSUER)
        .audience(List.of(AUDIENCE))
        .subject(userId.toString())
        .issuedAt(NOW)
        .notBefore(NOW)
        .expiresAt(NOW.plus(Duration.ofMinutes(15)))
        .claim("token_use", "access")
        .claim("auth_type", "LOGIN_JWT");
  }

  private String encode(JwtClaimsSet claims) {
    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  private void assertInvalidToken(String token, String statusCode) {
    assertThatThrownBy(() -> loginJwtService.authenticate(token))
        .isInstanceOf(CredentialAuthenticationException.class)
        .hasMessage(statusCode);
  }

  private UserAccount activeUser() {
    return UserAccount.create("Tester", "tester@example.com", null, NOW);
  }

  private static final class InMemoryUserAccountRepository implements UserAccountRepository {
    private final Map<UUID, UserAccount> users = new HashMap<>();

    @Override
    public Optional<UserAccount> findById(UUID id) {
      return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<UserAccount> findByIdForUpdate(UUID id) {
      return findById(id);
    }

    @Override
    public Optional<UserAccount> findByPrimaryEmailForUpdate(String primaryEmail) {
      return users.values().stream()
          .filter(user -> user.getPrimaryEmail().equalsIgnoreCase(primaryEmail))
          .findFirst();
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
      users.put(userAccount.getId(), userAccount);
      return userAccount;
    }
  }
}
