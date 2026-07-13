package app.domain.auth;

import java.util.Locale;
import java.util.regex.Pattern;

public record Sha256Hash(String value) {

  private static final Pattern SHA_256_HEX = Pattern.compile("[0-9a-fA-F]{64}");

  public Sha256Hash {
    if (value == null || !SHA_256_HEX.matcher(value).matches()) {
      throw new IllegalArgumentException("A SHA-256 hash must be 64 hexadecimal characters");
    }
    value = value.toLowerCase(Locale.ROOT);
  }

  @Override
  public String toString() {
    return "Sha256Hash[value=[REDACTED]]";
  }
}
