package app.domain;

import app.domain.apikey.ApiKey;
import app.domain.apikey.ApiKeyRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryApiKeyRepository implements ApiKeyRepository {
  private final Map<UUID, ApiKey> values = new LinkedHashMap<>();

  @Override
  public ApiKey save(ApiKey apiKey) {
    values.put(apiKey.getId(), apiKey);
    return apiKey;
  }

  @Override
  public List<ApiKey> findAll() {
    return new ArrayList<>(values.values());
  }

  @Override
  public List<ApiKey> findAllByUserIdOrderByIssuedAtDesc(UUID userId) {
    return values.values().stream()
        .filter(apiKey -> userId.equals(apiKey.getUserId()))
        .sorted(Comparator.comparing(ApiKey::getIssuedAt).reversed())
        .toList();
  }

  @Override
  public Optional<ApiKey> findByIdAndUserId(UUID id, UUID userId) {
    return Optional.ofNullable(values.get(id)).filter(apiKey -> userId.equals(apiKey.getUserId()));
  }

  @Override
  public Optional<ApiKey> findByKeyHash(String keyHash) {
    return values.values().stream()
        .filter(apiKey -> apiKey.getKeyHash().equals(keyHash))
        .findFirst();
  }

  @Override
  public boolean existsByKeyHash(String keyHash) {
    return findByKeyHash(keyHash).isPresent();
  }

  @Override
  public int claimUnownedByEmail(UUID userId, String email) {
    int claimed = 0;
    String normalized = email.toLowerCase(Locale.ROOT);
    for (ApiKey apiKey : values.values()) {
      if (apiKey.getUserId() == null
          && apiKey.getEmail().toLowerCase(Locale.ROOT).equals(normalized)) {
        setUserId(apiKey, userId);
        claimed++;
      }
    }
    return claimed;
  }

  private void setUserId(ApiKey apiKey, UUID userId) {
    try {
      var field = ApiKey.class.getDeclaredField("userId");
      field.setAccessible(true);
      field.set(apiKey, userId);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @Override
  public void updateRequestCount() {}
}
