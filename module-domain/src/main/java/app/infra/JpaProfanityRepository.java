package app.infra;

import app.domain.ProfanityWord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProfanityRepository extends JpaRepository<ProfanityWord, Long> {
}
