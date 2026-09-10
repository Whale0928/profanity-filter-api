package app.presentation;

import app.application.news.NewsService;
import app.application.news.NewsView;
import app.core.data.response.ApiResponse;
import app.core.data.response.PageView;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 인증 없이 접근하는 소식 조회 API입니다. 공개된 소식만 응답합니다. */
@RestController
@RequiredArgsConstructor
@Hidden
@RequestMapping(value = "/api/v1/news", produces = MediaType.APPLICATION_JSON_VALUE)
public class NewsController {

  private final NewsService newsService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageView<NewsView>>> list(
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String query,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    PageResult<NewsView> result =
        newsService.findPublished(category, query, PageQuery.of(page, size));
    return ApiResponse.ok(new PageView<>(result.items(), result.page(), result.hasNext()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<NewsView>> detail(@PathVariable Long id) {
    return ApiResponse.ok(newsService.findPublishedDetail(id));
  }
}
