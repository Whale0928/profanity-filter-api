package app.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AuthCodeExchangeRequest(@NotBlank String code) {
  @Override
  public String toString() {
    return "AuthCodeExchangeRequest[code=redacted]";
  }
}
