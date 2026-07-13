package app.domain.user;

import java.util.Arrays;

public enum OAuthProvider {
  GOOGLE("google"),
  GITHUB("github");

  private final String value;

  OAuthProvider(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static OAuthProvider from(String value) {
    if (value == null) {
      throw new IllegalArgumentException("OAuth provider must not be null");
    }
    return Arrays.stream(values())
        .filter(provider -> provider.value.equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported OAuth provider"));
  }
}
