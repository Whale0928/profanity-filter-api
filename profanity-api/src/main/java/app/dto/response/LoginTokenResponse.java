package app.dto.response;

public record LoginTokenResponse(
    String accessToken, String tokenType, long expiresIn, LoginUserResponse user) {
  @Override
  public String toString() {
    return "LoginTokenResponse[accessToken=redacted, tokenType="
        + tokenType
        + ", expiresIn="
        + expiresIn
        + ", userId="
        + user.id()
        + "]";
  }
}
