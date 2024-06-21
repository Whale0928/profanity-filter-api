package app.infra;

import app.domain.ProfanityRepository;
import app.domain.ProfanityWord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProfanityRepository extends ProfanityRepository, JpaRepository<ProfanityWord, Long> {
}
