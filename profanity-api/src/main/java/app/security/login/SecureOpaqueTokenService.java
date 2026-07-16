package app.security.login;

import app.domain.auth.Sha256Hash;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class SecureOpaqueTokenService {
  private static final int TOKEN_BYTES = 32;
  private final SecureRandom secureRandom = new SecureRandom();

  public OpaqueToken generate() {
    byte[] value = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(value);
    String plaintext = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    return new OpaqueToken(plaintext, hash(plaintext));
  }

  public Sha256Hash hash(String plaintext) {
    if (plaintext == null || plaintext.isBlank()) {
      throw new IllegalArgumentException("Opaque token must not be blank");
    }
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(plaintext.getBytes(StandardCharsets.UTF_8));
      return new Sha256Hash(HexFormat.of().formatHex(digest));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
    }
  }

  public record OpaqueToken(String plaintext, Sha256Hash hash) {
    @Override
    public String toString() {
      return "OpaqueToken[redacted]";
    }
  }
}
