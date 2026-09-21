package app.domain.manage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WordManagementRepository {
  Optional<WordManagementRequest> findById(Long id);

  WordManagementRequest save(WordManagementRequest request);

  List<WordManagementRequest> findAll();

  Optional<WordManagementRequest> findByInquiryId(Long inquiryId);

  /** 같은 단어 요청에 승인이 중복 적용되지 않도록 대상 행을 잠근 채 조회합니다. */
  Optional<WordManagementRequest> findByInquiryIdForUpdate(Long inquiryId);

  List<WordManagementRequest> findAllByInquiryIdIn(Collection<Long> inquiryIds);

  /** 아직 승인도 거절도 되지 않은 단어 요청 수입니다. */
  long countPendingRequests();

  Boolean activateWord(Long id);
}
