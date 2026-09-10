package app.application.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.InMemoryNewsPostRepository;
import app.domain.news.NewsCategory;
import app.domain.news.NewsPost;
import app.domain.news.NewsStatus;
import app.domain.support.PageQuery;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("공개 소식 조회")
class NewsServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-10T00:00:00Z");
  private static final UUID ADMIN_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

  private InMemoryNewsPostRepository repository;
  private NewsService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryNewsPostRepository();
    service = new NewsService(repository);
  }

  @Nested
  @DisplayName("목록을 조회하면")
  class ListTest {

    @Test
    @DisplayName("임시 저장 소식은 어떤 조건에서도 포함되지 않는다")
    void draftIsNeverExposed() {
      publish("공개 공지");
      draft("비공개 초안");

      var result = service.findPublished(null, null, PageQuery.of(null, null));

      assertThat(result.items()).extracting(NewsView::title).containsExactly("공개 공지");
    }

    @Test
    @DisplayName("검색어가 임시 저장 소식과 일치해도 결과에 나오지 않는다")
    void draftIsNeverExposedByQuery() {
      draft("비공개 초안");

      var result = service.findPublished(null, "초안", PageQuery.of(null, null));

      assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("본문은 내려보내지 않는다")
    void listOmitsContent() {
      publish("공개 공지");

      var result = service.findPublished(null, null, PageQuery.of(null, null));

      assertThat(result.items()).singleElement().extracting(NewsView::content).isNull();
    }

    @Test
    @DisplayName("분류로 걸러낸다")
    void filtersByCategory() {
      publish("공개 공지");

      var notice = service.findPublished("NOTICE", null, PageQuery.of(null, null));
      var changelog = service.findPublished("CHANGELOG", null, PageQuery.of(null, null));

      assertThat(notice.items()).hasSize(1);
      assertThat(changelog.items()).isEmpty();
    }
  }

  @Nested
  @DisplayName("상세를 조회하면")
  class DetailTest {

    @Test
    @DisplayName("공개된 소식은 본문까지 반환한다")
    void publishedDetailIncludesContent() {
      NewsPost saved = publish("공개 공지");

      var found = service.findPublishedDetail(saved.getId());

      assertThat(found.content()).isEqualTo("본문");
      assertThat(found.publishedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("임시 저장 소식은 존재해도 찾을 수 없다고 응답한다")
    void draftDetailIsNotFound() {
      NewsPost saved = draft("비공개 초안");

      assertThatThrownBy(() -> service.findPublishedDetail(saved.getId()))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.NEWS_NOT_FOUND.code());
    }

    @Test
    @DisplayName("없는 소식도 같은 오류로 응답한다")
    void missingDetailIsNotFound() {
      assertThatThrownBy(() -> service.findPublishedDetail(404L))
          .isInstanceOf(BusinessException.class);
    }
  }

  private NewsPost publish(String title) {
    return repository.save(
        NewsPost.write(title, NewsCategory.NOTICE, "본문", NewsStatus.PUBLISHED, ADMIN_ID, NOW));
  }

  private NewsPost draft(String title) {
    return repository.save(
        NewsPost.write(title, NewsCategory.NOTICE, "본문", NewsStatus.DRAFT, ADMIN_ID, NOW));
  }
}
