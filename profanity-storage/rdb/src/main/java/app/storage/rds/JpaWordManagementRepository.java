package app.storage.rds;

import app.domain.manage.WordManagementRepository;
import app.domain.manage.WordManagementRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaWordManagementRepository
    extends WordManagementRepository, JpaRepository<WordManagementRequest, Long> {

  @Override
  @Modifying
  @Query("UPDATE word_management SET status = 'OK' WHERE id = :id")
  Boolean activateWord(Long id);

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT w FROM word_management w WHERE w.inquiryId = :inquiryId")
  Optional<WordManagementRequest> findByInquiryIdForUpdate(@Param("inquiryId") Long inquiryId);
}
