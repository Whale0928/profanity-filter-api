package app.application.apikey;

import app.application.client.KeyGenerator;
import app.core.data.response.constant.StatusCode;
import app.domain.apikey.ApiKey;
import app.domain.apikey.ApiKeyMetadata;
import app.domain.apikey.ApiKeyRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiKeyReader implements ApiKeyMetadataReader {
  private final ApiKeyRepository apiKeyRepository;
  private final KeyGenerator keyGenerator;

  @Override
  @Transactional(readOnly = true)
  public ApiKeyMetadata read(String plaintextApiKey) {
    if (!keyGenerator.validateApiKey(plaintextApiKey)) {
      throw new IllegalArgumentException(StatusCode.INVALID_API_KEY.stringCode());
    }
    ApiKey apiKey =
        apiKeyRepository
            .findByKeyHash(keyGenerator.hashApiKey(plaintextApiKey))
            .filter(ApiKey::isActive)
            .orElseThrow(
                () -> new NoSuchElementException(StatusCode.NOT_FOUND_CLIENT.stringCode()));
    return new ApiKeyMetadata(
        apiKey.getId(),
        apiKey.getEmail(),
        apiKey.getIssuerInfo(),
        apiKey.plainPermissions(),
        apiKey.getIssuedAt().toString(),
        apiKey.getKeyHash());
  }
}
