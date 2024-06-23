package app.domain;

import java.util.Optional;

public interface RecordRepository {

    Optional<Records> findById(Long id);

    Records save(Records records);

    void delete(Records records);
}
