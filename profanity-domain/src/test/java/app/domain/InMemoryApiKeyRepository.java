package app.domain;

import app.domain.apikey.ApiKey;
import app.domain.apikey.ApiKeyRepository;
import app.domain.support.PageResult;
import java.util.ArrayList;
import java.util.Collection;
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
  public Optional<ApiKey> findById(UUID id) {
    return Optional.ofNullable(values.get(id));
  }

  @Override
  public Optional<ApiKey> findByIdForUpdate(UUID id) {
    return findById(id);
  }

  @Override
  public PageResult<ApiKey> searchForAdmin(String query, Boolean activeOnly, int page, int size) {
    List<ApiKey> matched =
        values.values().stream()
            .filter(apiKey -> activeOnly == null || apiKey.isActive() == activeOnly)
            .filter(apiKey -> query == null || matchesQuery(apiKey, query))
            .sorted(Comparator.comparing(ApiKey::getIssuedAt).reversed())
            .toList();
    int from = Math.min(page * size, matched.size());
    int to = Math.min(from + size, matched.size());
    return PageResult.of(matched.subList(from, to), page, to < matched.size());
  }

  private static boolean matchesQuery(ApiKey apiKey, String query) {
    String keyword = query.toLowerCase(Locale.ROOT);
    return apiKey.getName().toLowerCase(Locale.ROOT).contains(keyword)
        || apiKey.getEmail().toLowerCase(Locale.ROOT).contains(keyword)
        || apiKey.getKeyHint().toLowerCase(Locale.ROOT).contains(keyword);
  }

  @Override
  public Optional<ApiKey> findByIdAndUserId(UUID id, UUID userId) {
    return Optional.ofNullable(values.get(id)).filter(apiKey -> userId.equals(apiKey.getUserId()));
  }

  @Override
  public Optional<ApiKey> findByIdAndUserIdForUpdate(UUID id, UUID userId) {
    return findByIdAndUserId(id, userId);
  }

  @Override
  public long countApiKeys() {
    return values.size();
  }

  @Override
  public long countActiveApiKeys() {
    return values.values().stream().filter(ApiKey::isActive).count();
  }

  @Override
  public List<ApiKey> findAllByKeyHashIn(Collection<String> keyHashes) {
    return values.values().stream()
        .filter(apiKey -> keyHashes.contains(apiKey.getKeyHash()))
        .toList();
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
