package app.domain.user;

import java.util.Objects;

public record OAuthLoginProfile(
    OAuthProvider provider,
    String providerUserId,
    String providerEmail,
    boolean emailVerified,
    boolean emailAuthoritative,
    String providerUsername,
    String displayName,
    String avatarUrl) {

  public OAuthLoginProfile {
    Objects.requireNonNull(provider, "provider must not be null");
    providerUserId = requireText(providerUserId, "providerUserId");
    displayName = requireText(displayName, "displayName");
    providerEmail = blankToNull(providerEmail);
    providerUsername = blankToNull(providerUsername);
    avatarUrl = blankToNull(avatarUrl);
  }

  @Override
  public String toString() {
    return "OAuthLoginProfile[provider=" + provider + ", identity=[REDACTED]]";
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.trim();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
