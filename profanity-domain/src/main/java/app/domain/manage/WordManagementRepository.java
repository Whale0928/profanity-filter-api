package app.domain.manage;

import java.util.List;
import java.util.Optional;

public interface WordManagementRepository {
    Optional<WordManagementRequest> findById(Long id);

    WordManagementRequest save(WordManagementRequest request);

    List<WordManagementRequest> findAll();

    Boolean activateWord(Long id);
}
