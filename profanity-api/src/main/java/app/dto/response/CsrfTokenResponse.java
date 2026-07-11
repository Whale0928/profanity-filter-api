package app.dto.response;

public record CsrfTokenResponse(String headerName, String token) {
  @Override
  public String toString() {
    return "CsrfTokenResponse[headerName=" + headerName + ", token=redacted]";
  }
}
