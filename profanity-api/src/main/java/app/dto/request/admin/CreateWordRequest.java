package app.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWordRequest(@NotBlank @Size(max = 255) String word) {}
