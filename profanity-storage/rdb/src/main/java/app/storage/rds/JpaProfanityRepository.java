package app.storage.rds;

import app.domain.profanity.ProfanityRepository;
import app.domain.profanity.ProfanityWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaProfanityRepository extends ProfanityRepository, JpaRepository<ProfanityWord, Long> {

    @Override
    @Query("SELECT COUNT(p) FROM profanity_word p")
    long countAll();
}
