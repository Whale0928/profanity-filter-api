package app.domain.news;

import app.domain.support.PageResult;
import java.util.Optional;

public interface NewsPostRepository {
  NewsPost save(NewsPost newsPost);

  Optional<NewsPost> findById(Long id);

  void delete(NewsPost newsPost);

  /**
   * 공개된 소식만 조회합니다. 비공개 소식은 어떤 조건에서도 포함되지 않습니다.
   *
   * @param category 분류 필터. null이면 전체입니다.
   * @param query 제목과 본문에 대한 부분 일치 검색어. null이면 전체입니다.
   */
  PageResult<NewsPost> searchPublished(NewsCategory category, String query, int page, int size);

  /**
   * 관리자 목록을 조회합니다. 임시 저장 상태를 포함합니다.
   *
   * @param status 공개 상태 필터. null이면 전체입니다.
   * @param category 분류 필터. null이면 전체입니다.
   * @param query 제목과 본문에 대한 부분 일치 검색어. null이면 전체입니다.
   */
  PageResult<NewsPost> searchForAdmin(
      NewsStatus status, NewsCategory category, String query, int page, int size);
}
