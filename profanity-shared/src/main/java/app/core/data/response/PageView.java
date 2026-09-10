package app.core.data.response;

import java.util.List;

/**
 * 관리자와 대시보드 목록 API의 공통 응답 형식입니다. 전체 건수를 세지 않고 다음 페이지 존재 여부만 알려 줍니다.
 *
 * @param items 현재 페이지 항목. 조회 결과가 없으면 빈 목록입니다.
 * @param page 0부터 시작하는 현재 페이지 번호
 * @param hasNext 다음 페이지가 있으면 true
 */
public record PageView<T>(List<T> items, int page, boolean hasNext) {

  public PageView {
    items = items == null ? List.of() : List.copyOf(items);
  }

  public static <T> PageView<T> of(List<T> items, int page, boolean hasNext) {
    return new PageView<>(items, page, hasNext);
  }
}
