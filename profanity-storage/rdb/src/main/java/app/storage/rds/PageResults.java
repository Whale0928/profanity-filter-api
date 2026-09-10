package app.storage.rds;

import app.domain.support.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

/** Spring Data Slice를 도메인 계약의 PageResult로 변환합니다. */
public final class PageResults {

  private PageResults() {}

  public static PageRequest request(int page, int size, Sort sort) {
    return PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);
  }

  public static <T> PageResult<T> from(Slice<T> slice) {
    return PageResult.of(slice.getContent(), slice.getNumber(), slice.hasNext());
  }
}
