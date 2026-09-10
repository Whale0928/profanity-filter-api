package app.application.news;

import app.domain.news.NewsPost;
import java.time.Instant;

/**
 * 소식 응답 항목입니다.
 *
 * <p>공개 목록에서는 본문을 내려보내지 않으므로 content가 null입니다. 상세 조회와 관리자 목록에서는 본문을 포함합니다.
 */
public record NewsView(
    Long id,
    String title,
    String category,
    String content,
    String status,
    Instant createdAt,
    Instant updatedAt,
    Instant publishedAt) {

  public static NewsView withContent(NewsPost newsPost) {
    return of(newsPost, newsPost.getContentMarkdown());
  }

  public static NewsView withoutContent(NewsPost newsPost) {
    return of(newsPost, null);
  }

  private static NewsView of(NewsPost newsPost, String content) {
    return new NewsView(
        newsPost.getId(),
        newsPost.getTitle(),
        newsPost.getCategory().name(),
        content,
        newsPost.getStatus().name(),
        newsPost.getCreatedAt(),
        newsPost.getUpdatedAt(),
        newsPost.getPublishedAt());
  }
}
