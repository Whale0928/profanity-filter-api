package app.dto.request;

import app.core.data.constant.Mode;

public record FilterRequest(
    String text, Mode mode, String apiKeyHash, String clientIp, String referrer) {
  public static FilterRequest create(
      String text, Mode mode, String apiKeyHash, String clientIp, String referrer) {
    return new FilterRequest(text, mode, apiKeyHash, clientIp, referrer);
  }

  @Override
  public String toString() {
    return "FilterRequest{"
        + "text='"
        + text
        + '\''
        + ", mode="
        + mode
        + ", apiKeyHash='"
        + apiKeyHash
        + '\''
        + ", clientIp='"
        + clientIp
        + '\''
        + ", referrer='"
        + referrer
        + '\''
        + '}';
  }
}
