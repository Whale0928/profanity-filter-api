package app.domain.support;

import java.util.List;
import java.util.function.Function;

/**
 * 도메인 저장소의 목록 조회 결과입니다. 전체 건수를 세지 않고 다음 페이지 존재 여부만 알려 줍니다.
 *
 * <p>API 응답 형식은 profanity-shared의 PageView가 담당하며 이 타입은 도메인 내부에서만 사용합니다.
 *
 * @param items 현재 페이지 항목. 조회 결과가 없으면 빈 목록입니다.
 * @param page 0부터 시작하는 현재 페이지 번호
 * @param hasNext 다음 페이지가 있으면 true
 */
public record PageResult<T>(List<T> items, int page, boolean hasNext) {

  public PageResult {
    items = items == null ? List.of() : List.copyOf(items);
  }

  public static <T> PageResult<T> of(List<T> items, int page, boolean hasNext) {
    return new PageResult<>(items, page, hasNext);
  }

  public static <T> PageResult<T> empty(int page) {
    return new PageResult<>(List.of(), page, false);
  }

  public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
    List<R> mapped = items.stream().<R>map(mapper).toList();
    return new PageResult<>(mapped, page, hasNext);
  }
}
