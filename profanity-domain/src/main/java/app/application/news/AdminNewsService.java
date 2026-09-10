package app.application.news;

import app.application.admin.AdminAuditAction;
import app.application.admin.AdminAuditService;
import app.application.admin.AdminAuditTargetType;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.news.NewsCategory;
import app.domain.news.NewsPost;
import app.domain.news.NewsPostRepository;
import app.domain.news.NewsStatus;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 소식 관리 기능입니다. 목록에는 임시 저장 소식도 포함합니다. */
@Service
@RequiredArgsConstructor
public class AdminNewsService {

  private final NewsPostRepository newsPostRepository;
  private final AdminAuditService adminAuditService;
  private final Clock loginAuthClock;

  @Transactional(readOnly = true)
  public PageResult<NewsView> search(
      String status, String category, String query, PageQuery pageQuery) {
    NewsStatus statusFilter = NewsStatus.parseFilter(status);
    NewsCategory categoryFilter = NewsCategory.parseFilter(category);
    return newsPostRepository
        .searchForAdmin(
            statusFilter,
            categoryFilter,
            PageQuery.normalizeQuery(query),
            pageQuery.page(),
            pageQuery.size())
        .map(NewsView::withContent);
  }

  @Transactional
  public NewsView create(UUID actorId, NewsWriteCommand command) {
    Instant now = loginAuthClock.instant();
    NewsPost saved =
        newsPostRepository.save(
            NewsPost.write(
                command.title(),
                command.category(),
                command.content(),
                command.status(),
                actorId,
                now));
    record(actorId, AdminAuditAction.NEWS_CREATED, saved, now);
    return NewsView.withContent(saved);
  }

  @Transactional
  public NewsView update(UUID actorId, Long id, NewsWriteCommand command) {
    NewsPost newsPost = requirePost(id);
    Instant now = loginAuthClock.instant();
    newsPost.modify(
        command.title(), command.category(), command.content(), command.status(), actorId, now);
    NewsPost saved = newsPostRepository.save(newsPost);
    record(actorId, AdminAuditAction.NEWS_UPDATED, saved, now);
    return NewsView.withContent(saved);
  }

  @Transactional
  public void delete(UUID actorId, Long id) {
    NewsPost newsPost = requirePost(id);
    Instant now = loginAuthClock.instant();
    newsPostRepository.delete(newsPost);
    record(actorId, AdminAuditAction.NEWS_DELETED, newsPost, now);
  }

  private NewsPost requirePost(Long id) {
    return newsPostRepository
        .findById(id)
        .orElseThrow(() -> new BusinessException(StatusCode.NEWS_NOT_FOUND));
  }

  private void record(UUID actorId, String action, NewsPost newsPost, Instant now) {
    adminAuditService.record(
        actorId,
        action,
        AdminAuditTargetType.NEWS,
        String.valueOf(newsPost.getId()),
        newsPost.getStatus().name(),
        now);
  }
}
