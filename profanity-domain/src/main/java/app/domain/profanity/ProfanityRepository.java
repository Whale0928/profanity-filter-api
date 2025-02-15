package app.domain.profanity;

import java.util.List;
import java.util.Optional;

public interface ProfanityRepository {

    Optional<ProfanityWord> findById(Long id);

    ProfanityWord save(ProfanityWord profanityWord);

    List<ProfanityWord> findAll();

    void deleteAll();

    long countAll();
}
