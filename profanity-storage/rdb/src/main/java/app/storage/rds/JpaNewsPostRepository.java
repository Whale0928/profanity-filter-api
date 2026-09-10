package app.storage.rds;

import app.domain.news.NewsCategory;
import app.domain.news.NewsPost;
import app.domain.news.NewsPostRepository;
import app.domain.news.NewsStatus;
import app.domain.support.PageResult;
import java.util.Collection;
import java.util.EnumSet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaNewsPostRepository extends NewsPostRepository, JpaRepository<NewsPost, Long> {

  /**
   * 상태와 분류 필터를 IN 조건으로 넘겨 JPQL에서 enum 파라미터를 null과 비교하지 않도록 합니다. 필터가 없으면 전체 값 집합을 넘깁니다.
   *
   * <p>공개 목록은 PUBLISHED만 담은 집합을 넘기므로 비공개 소식이 조회 결과에 섞일 수 없습니다.
   */
  @Override
  default PageResult<NewsPost> searchPublished(
      NewsCategory category, String query, int page, int size) {
    Pageable pageable =
        PageResults.request(
            page, size, Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));
    return PageResults.from(
        searchSlice(EnumSet.of(NewsStatus.PUBLISHED), categoryFilter(category), query, pageable));
  }

  @Override
  default PageResult<NewsPost> searchForAdmin(
      NewsStatus status, NewsCategory category, String query, int page, int size) {
    Pageable pageable = PageResults.request(page, size, Sort.by(Sort.Direction.DESC, "id"));
    return PageResults.from(
        searchSlice(statusFilter(status), categoryFilter(category), query, pageable));
  }

  private static Collection<NewsStatus> statusFilter(NewsStatus status) {
    return status == null ? EnumSet.allOf(NewsStatus.class) : EnumSet.of(status);
  }

  private static Collection<NewsCategory> categoryFilter(NewsCategory category) {
    return category == null ? EnumSet.allOf(NewsCategory.class) : EnumSet.of(category);
  }

  @Query(
      """
      select n from news_posts n
      where n.status in :statuses
        and n.category in :categories
        and (:query is null
             or lower(n.title) like lower(concat('%', :query, '%'))
             or lower(n.contentMarkdown) like lower(concat('%', :query, '%')))
      """)
  Slice<NewsPost> searchSlice(
      @Param("statuses") Collection<NewsStatus> statuses,
      @Param("categories") Collection<NewsCategory> categories,
      @Param("query") String query,
      Pageable pageable);
}
