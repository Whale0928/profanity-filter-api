package app.domain.inquiry;

import app.domain.support.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface InquiryRepository {
  Inquiry save(Inquiry inquiry);

  Optional<Inquiry> findById(Long id);

  /** 문의 상태와 단어 승인을 동시에 처리하지 못하도록 대상 행을 잠근 채 조회합니다. */
  Optional<Inquiry> findByIdForUpdate(Long id);

  /**
   * 관리자 문의 목록을 조회합니다.
   *
   * @param type 유형 필터. null이면 전체입니다.
   * @param status 상태 필터. null이면 전체입니다.
   * @param query 제목과 내용에 대한 부분 일치 검색어. null이면 전체입니다.
   */
  PageResult<Inquiry> searchForAdmin(
      InquiryType type, InquiryStatus status, String query, int page, int size);

  /** 로그인 사용자가 등록한 문의만 조회합니다. */
  PageResult<Inquiry> searchByRequester(UUID requesterUserId, String query, int page, int size);
}
