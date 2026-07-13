package app.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtKeyConfigTest {

  private final JwtKeyConfig config = new JwtKeyConfig();

  @Test
  @DisplayName("운영 환경에 로그인 JWT private JWK가 없으면 시작을 거부한다")
  void jwtKeyMaterial_whenPrivateJwkIsMissingInProduction_throwsIllegalStateException() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");

    assertThatThrownBy(() -> config.jwtKeyMaterial(new JwtKeyProperties("", ""), environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("LOGIN_JWT_PRIVATE_JWK");
  }

  @Test
  @DisplayName("현재와 이전 public JWK를 함께 설정하면 rotation 이전 token도 검증한다")
  void loginJwtDecoder_whenPreviousPublicKeyIsConfigured_verifiesBothKeyGenerations()
      throws Exception {
    RSAKey current = new RSAKeyGenerator(2048).keyID("current-key").generate();
    RSAKey previous = new RSAKeyGenerator(2048).keyID("previous-key").generate();
    String publicJwkSet =
        new JWKSet(List.of(current.toPublicJWK(), previous.toPublicJWK())).toString(false);
    JwtKeyConfig.JwtKeyMaterial material =
        config.jwtKeyMaterial(
            new JwtKeyProperties(current.toJSONString(), publicJwkSet), new MockEnvironment());

    String currentToken = encode(config.loginJwtEncoder(material), "current-key");
    String previousToken =
        encode(new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(previous))), "previous-key");

    assertThat(config.loginJwtDecoder(material).decode(currentToken).getSubject())
        .isEqualTo("current-user");
    assertThat(config.loginJwtDecoder(material).decode(previousToken).getSubject())
        .isEqualTo("previous-user");
  }

  @Test
  @DisplayName("검증 JWK set에 현재 signing key가 없으면 구성을 거부한다")
  void jwtKeyMaterial_whenPublicSetOmitsActiveKey_throwsIllegalStateException() throws Exception {
    RSAKey current = new RSAKeyGenerator(2048).keyID("current-key").generate();
    RSAKey previous = new RSAKeyGenerator(2048).keyID("previous-key").generate();

    assertThatThrownBy(
            () ->
                config.jwtKeyMaterial(
                    new JwtKeyProperties(
                        current.toJSONString(), new JWKSet(previous.toPublicJWK()).toString(false)),
                    new MockEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("active private key kid");
  }

  @Test
  @DisplayName("검증 JWK의 kid만 같고 RSA key material이 다르면 구성을 거부한다")
  void jwtKeyMaterial_whenPublicSetReusesKidWithDifferentKey_throwsIllegalStateException()
      throws Exception {
    RSAKey current = new RSAKeyGenerator(2048).keyID("active-key").generate();
    RSAKey mismatched = new RSAKeyGenerator(2048).keyID("active-key").generate();

    assertThatThrownBy(
            () ->
                config.jwtKeyMaterial(
                    new JwtKeyProperties(
                        current.toJSONString(),
                        new JWKSet(mismatched.toPublicJWK()).toString(false)),
                    new MockEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("active private key kid");
  }

  private String encode(JwtEncoder encoder, String keyId) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(keyId.replace("key", "user"))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .build();
    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).type("JWT").build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
