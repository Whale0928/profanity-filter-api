package app.storage.rds;

import app.domain.manage.WordManagementRepository;
import app.domain.manage.WordManagementRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaWordManagementRepository extends WordManagementRepository, JpaRepository<WordManagementRequest, Long> {

    @Override
    @Modifying
    @Query("UPDATE word_management SET status = 'OK' WHERE id = :id")
    Boolean activateWord(Long id);
}
