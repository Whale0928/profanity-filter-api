package app.application.news;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.news.NewsCategory;
import app.domain.news.NewsPost;
import app.domain.news.NewsPostRepository;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 없이 접근하는 소식 조회 기능입니다.
 *
 * <p>비공개 소식이 외부로 새어 나가지 않도록 목록은 공개 상태만 조회하고, 상세도 공개 상태가 아니면 존재 여부를 구분하지 않고 동일한 오류를 반환합니다.
 */
@Service
@RequiredArgsConstructor
public class NewsService {

  private final NewsPostRepository newsPostRepository;

  @Transactional(readOnly = true)
  public PageResult<NewsView> findPublished(String category, String query, PageQuery pageQuery) {
    NewsCategory categoryFilter = NewsCategory.parseFilter(category);
    return newsPostRepository
        .searchPublished(
            categoryFilter, PageQuery.normalizeQuery(query), pageQuery.page(), pageQuery.size())
        .map(NewsView::withoutContent);
  }

  /**
   * 공개된 소식 하나를 조회합니다.
   *
   * @throws BusinessException 소식이 없거나 아직 공개되지 않은 경우
   */
  @Transactional(readOnly = true)
  public NewsView findPublishedDetail(Long id) {
    NewsPost newsPost =
        newsPostRepository
            .findById(id)
            .filter(NewsPost::isPublished)
            .orElseThrow(() -> new BusinessException(StatusCode.NEWS_NOT_FOUND));
    return NewsView.withContent(newsPost);
  }
}
