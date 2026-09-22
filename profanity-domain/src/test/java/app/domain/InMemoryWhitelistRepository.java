package app.domain;

import app.domain.whitelist.Whitelist;
import app.domain.whitelist.WhitelistRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryWhitelistRepository implements WhitelistRepository {

  private final Map<UUID, Whitelist> values = new LinkedHashMap<>();

  @Override
  public Whitelist save(Whitelist whitelist) {
    values.put(whitelist.getId(), whitelist);
    return whitelist;
  }

  @Override
  public Optional<Whitelist> findById(UUID id) {
    return Optional.ofNullable(values.get(id));
  }

  @Override
  public List<Whitelist> findAllByUserIdOrderByCreatedAtAscIdAsc(UUID userId) {
    return values.values().stream()
        .filter(whitelist -> whitelist.isOwnedBy(userId))
        .sorted(Comparator.comparing(Whitelist::getCreatedAt).thenComparing(Whitelist::getId))
        .toList();
  }

  @Override
  public long countByUserId(UUID userId) {
    return values.values().stream().filter(whitelist -> whitelist.isOwnedBy(userId)).count();
  }

  @Override
  public void delete(Whitelist whitelist) {
    values.remove(whitelist.getId());
  }
}
