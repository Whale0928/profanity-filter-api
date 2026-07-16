package app.dto.request;

import app.application.apikey.ApiKeyManagementService.CreateApiKeyCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 255) String issuerInfo,
    @Size(max = 255) String note) {
  public CreateApiKeyCommand toCommand() {
    return new CreateApiKeyCommand(name, issuerInfo, note);
  }
}
