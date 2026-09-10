package app.storage.rds;

import app.domain.inquiry.Inquiry;
import app.domain.inquiry.InquiryRepository;
import app.domain.inquiry.InquiryStatus;
import app.domain.inquiry.InquiryType;
import app.domain.support.PageResult;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaInquiryRepository extends InquiryRepository, JpaRepository<Inquiry, Long> {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM inquiries i WHERE i.id = :id")
  Optional<Inquiry> findByIdForUpdate(@Param("id") Long id);

  /** 유형과 상태 필터를 IN 조건으로 넘겨 JPQL에서 enum 파라미터를 null과 비교하지 않도록 합니다. */
  @Override
  default PageResult<Inquiry> searchForAdmin(
      InquiryType type, InquiryStatus status, String query, int page, int size) {
    Pageable pageable = PageResults.request(page, size, Sort.by(Sort.Direction.DESC, "id"));
    return PageResults.from(searchSlice(typeFilter(type), statusFilter(status), query, pageable));
  }

  @Override
  default PageResult<Inquiry> searchByRequester(
      UUID requesterUserId, String query, int page, int size) {
    Pageable pageable = PageResults.request(page, size, Sort.by(Sort.Direction.DESC, "id"));
    return PageResults.from(searchRequesterSlice(requesterUserId, query, pageable));
  }

  private static Collection<InquiryType> typeFilter(InquiryType type) {
    return type == null ? EnumSet.allOf(InquiryType.class) : EnumSet.of(type);
  }

  private static Collection<InquiryStatus> statusFilter(InquiryStatus status) {
    return status == null ? EnumSet.allOf(InquiryStatus.class) : EnumSet.of(status);
  }

  @Query(
      """
      select i from inquiries i
      where i.type in :types
        and i.status in :statuses
        and (:query is null
             or lower(i.title) like lower(concat('%', :query, '%'))
             or lower(i.content) like lower(concat('%', :query, '%')))
      """)
  Slice<Inquiry> searchSlice(
      @Param("types") Collection<InquiryType> types,
      @Param("statuses") Collection<InquiryStatus> statuses,
      @Param("query") String query,
      Pageable pageable);

  @Query(
      """
      select i from inquiries i
      where i.requesterUserId = :requesterUserId
        and (:query is null
             or lower(i.title) like lower(concat('%', :query, '%'))
             or lower(i.content) like lower(concat('%', :query, '%')))
      """)
  Slice<Inquiry> searchRequesterSlice(
      @Param("requesterUserId") UUID requesterUserId,
      @Param("query") String query,
      Pageable pageable);
}
