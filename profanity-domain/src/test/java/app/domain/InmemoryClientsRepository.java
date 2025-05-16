package app.domain;

import app.domain.client.Clients;
import app.domain.client.ClientsRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InmemoryClientsRepository implements ClientsRepository {
    private final Map<UUID, Clients> store = new HashMap<>();

    @Override
    public Clients save(Clients client) {
        store.put(client.getId(), client);
        return client;
    }

    @Override
    public List<Clients> findAll() {
        return store.values().stream().toList();
    }

    @Override
    public void deleteById(UUID id) {
        store.remove(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return store.containsKey(id);
    }

    @Override
    public Optional<Clients> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsByEmail(String email) {
        return store.values().stream()
                .anyMatch(client -> client.getEmail().equals(email));
    }

    @Override
    public boolean existsByApiKey(String apiKey) {
        return store.values().stream()
                .anyMatch(client -> client.getApiKey().equals(apiKey));
    }

    @Override
    public Optional<Clients> findByEmail(String email) {
        return store.values().stream()
                .filter(client -> client.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public Optional<Clients> findByApiKey(String apiKey) {
        return store.values().stream()
                .filter(client -> client.getApiKey().equals(apiKey))
                .findFirst();
    }

    @Override
    public Optional<Clients> findByEmailAndApiKey(String email, String apiKey) {
        return store.values().stream()
                .filter(client -> client.getEmail().equals(email) && client.getApiKey().equals(apiKey))
                .findFirst();
    }

    @Override
    public Optional<Clients> findByIssuerInfo(String issuerInfo) {
        return store.values().stream()
                .filter(client -> client.getIssuerInfo().equals(issuerInfo))
                .findFirst();
    }

    @Override
    // This method is intentionally left empty as this is a test-specific implementation
    // of the ClientsRepository interface. It is not required for in-memory testing.
    public void updateClientRequestCount() {
    }

    // 테스트를 위한 추가 메서드
    public void clear() {
        store.clear();
    }
}
