package app.infra;

import app.domain.profanity.ProfanityRepository;
import app.domain.profanity.ProfanityWord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProfanityRepository extends ProfanityRepository, JpaRepository<ProfanityWord, Long> {
}
