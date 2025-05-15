package app.domain.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository {
    Optional<Report> findById(UUID id);

    Report save(Report Report);

    <S extends Report> List<S> saveAll(Iterable<S> entities);

    List<Report> findAll();

    List<Report> findAllByClientId(UUID clientId);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
