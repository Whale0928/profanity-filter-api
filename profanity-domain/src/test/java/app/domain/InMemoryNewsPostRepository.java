package app.domain;

import app.domain.news.NewsCategory;
import app.domain.news.NewsPost;
import app.domain.news.NewsPostRepository;
import app.domain.news.NewsStatus;
import app.domain.support.PageResult;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryNewsPostRepository implements NewsPostRepository {

  private final Map<Long, NewsPost> values = new LinkedHashMap<>();
  private final AtomicLong sequence = new AtomicLong();

  @Override
  public NewsPost save(NewsPost newsPost) {
    if (newsPost.getId() == null) {
      assignId(newsPost, sequence.incrementAndGet());
    }
    values.put(newsPost.getId(), newsPost);
    return newsPost;
  }

  @Override
  public Optional<NewsPost> findById(Long id) {
    return Optional.ofNullable(values.get(id));
  }

  @Override
  public void delete(NewsPost newsPost) {
    values.remove(newsPost.getId());
  }

  @Override
  public PageResult<NewsPost> searchPublished(
      NewsCategory category, String query, int page, int size) {
    List<NewsPost> matched =
        values.values().stream()
            .filter(NewsPost::isPublished)
            .filter(post -> category == null || post.getCategory() == category)
            .filter(post -> matches(post, query))
            .sorted(
                Comparator.comparing(
                        NewsPost::getPublishedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(NewsPost::getId)
                    .reversed())
            .toList();
    return slice(matched, page, size);
  }

  @Override
  public PageResult<NewsPost> searchForAdmin(
      NewsStatus status, NewsCategory category, String query, int page, int size) {
    List<NewsPost> matched =
        values.values().stream()
            .filter(post -> status == null || post.getStatus() == status)
            .filter(post -> category == null || post.getCategory() == category)
            .filter(post -> matches(post, query))
            .sorted(Comparator.comparing(NewsPost::getId).reversed())
            .toList();
    return slice(matched, page, size);
  }

  private static boolean matches(NewsPost post, String query) {
    if (query == null) {
      return true;
    }
    String keyword = query.toLowerCase(Locale.ROOT);
    return post.getTitle().toLowerCase(Locale.ROOT).contains(keyword)
        || post.getContentMarkdown().toLowerCase(Locale.ROOT).contains(keyword);
  }

  private static PageResult<NewsPost> slice(List<NewsPost> matched, int page, int size) {
    int from = Math.min(page * size, matched.size());
    int to = Math.min(from + size, matched.size());
    return PageResult.of(matched.subList(from, to), page, to < matched.size());
  }

  private static void assignId(NewsPost newsPost, long id) {
    try {
      Field field = NewsPost.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(newsPost, id);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
