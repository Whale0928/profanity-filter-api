package app.application.apikey;

import app.domain.apikey.ApiKeyRepository;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiKeyOwnershipService {
  private final ApiKeyRepository apiKeyRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int claimUnownedKeys(UUID userId, String verifiedEmail) {
    if (userId == null || verifiedEmail == null || verifiedEmail.isBlank()) {
      throw new IllegalArgumentException("userId and verifiedEmail are required");
    }
    return apiKeyRepository.claimUnownedByEmail(
        userId, verifiedEmail.trim().toLowerCase(Locale.ROOT));
  }
}
