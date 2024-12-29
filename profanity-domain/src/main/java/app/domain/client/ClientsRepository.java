package app.domain.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientsRepository {
    Optional<Clients> findById(UUID id);

    Clients save(Clients clients);

    List<Clients> findAll();

    void deleteById(UUID id);

    boolean existsById(UUID id);

    boolean existsByEmail(String email);

    boolean existsByApiKey(String apiKey);

    Optional<Clients> findByEmail(String email);

    Optional<Clients> findByApiKey(String apiKey);

    Optional<Clients> findByEmailAndApiKey(String email, String apiKey);

    Optional<Clients> findByIssuerInfo(String issuerInfo);
}
