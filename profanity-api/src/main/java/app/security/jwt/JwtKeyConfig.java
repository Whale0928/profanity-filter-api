package app.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Slf4j
@Configuration
@EnableConfigurationProperties({JwtKeyProperties.class, JwtSecurityProperties.class})
public class JwtKeyConfig {

  @Bean
  JwtKeyMaterial jwtKeyMaterial(JwtKeyProperties properties, Environment environment) {
    if (hasText(properties.privateJwk())) {
      return configuredKeyMaterial(properties);
    }
    if (environment.acceptsProfiles(Profiles.of("prod"))) {
      throw new IllegalStateException("LOGIN_JWT_PRIVATE_JWK must be configured in prod");
    }
    log.warn("LOGIN_JWT_PRIVATE_JWK is absent; using an ephemeral RSA key outside prod");
    return ephemeralKeyMaterial();
  }

  @Bean
  JwtEncoder loginJwtEncoder(JwtKeyMaterial keyMaterial) {
    JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(keyMaterial.privateKey()));
    return new NimbusJwtEncoder(source);
  }

  @Bean
  JwtDecoder loginJwtDecoder(JwtKeyMaterial keyMaterial) {
    JWKSource<SecurityContext> source =
        new ImmutableJWKSet<>(new JWKSet(keyMaterial.verificationKeys()));
    JWSKeySelector<SecurityContext> selector =
        new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, source);
    DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(selector);
    return new NimbusJwtDecoder(processor);
  }

  private JwtKeyMaterial configuredKeyMaterial(JwtKeyProperties properties) {
    try {
      RSAKey privateKey = RSAKey.parse(properties.privateJwk());
      if (!privateKey.isPrivate() || !hasText(privateKey.getKeyID())) {
        throw new IllegalStateException("LOGIN_JWT_PRIVATE_JWK requires private key data and kid");
      }

      List<JWK> verificationKeys =
          hasText(properties.publicJwkSet())
              ? JWKSet.parse(properties.publicJwkSet()).getKeys().stream()
                  .map(key -> (JWK) requireRsaPublicKey(key))
                  .toList()
              : List.of(privateKey.toPublicJWK());
      RSAKey activePublicKey = privateKey.toPublicJWK();
      boolean activeKeyPresent =
          verificationKeys.stream()
              .map(RSAKey.class::cast)
              .anyMatch(
                  key ->
                      privateKey.getKeyID().equals(key.getKeyID())
                          && activePublicKey.getModulus().equals(key.getModulus())
                          && activePublicKey.getPublicExponent().equals(key.getPublicExponent()));
      if (!activeKeyPresent) {
        throw new IllegalStateException(
            "LOGIN_JWT_PUBLIC_JWK_SET does not contain the active private key kid");
      }
      return new JwtKeyMaterial(privateKey, verificationKeys);
    } catch (ParseException exception) {
      throw new IllegalStateException("Failed to parse login JWT JWK configuration", exception);
    }
  }

  private JwtKeyMaterial ephemeralKeyMaterial() {
    try {
      RSAKey privateKey = new RSAKeyGenerator(2048).keyID("local-" + UUID.randomUUID()).generate();
      return new JwtKeyMaterial(privateKey, List.of(privateKey.toPublicJWK()));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to generate ephemeral login JWT key", exception);
    }
  }

  private RSAKey requireRsaPublicKey(JWK key) {
    JWK publicKey = key.toPublicJWK();
    if (!(publicKey instanceof RSAKey rsaKey) || !hasText(rsaKey.getKeyID())) {
      throw new IllegalStateException(
          "LOGIN_JWT_PUBLIC_JWK_SET must contain public RSA keys with kid");
    }
    return rsaKey;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  record JwtKeyMaterial(RSAKey privateKey, List<JWK> verificationKeys) {}
}
