package app.domain.support;

/**
 * 관리자 목록 API의 공통 페이지 파라미터입니다. page는 0부터 시작하고 size 기본값은 20, 최대값은 100입니다.
 *
 * @param page 0 이상으로 정규화된 페이지 번호
 * @param size 1 이상 100 이하로 정규화된 페이지 크기
 */
public record PageQuery(int page, int size) {
  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  /** null과 범위를 벗어난 값을 계약 기본값으로 정규화합니다. */
  public static PageQuery of(Integer page, Integer size) {
    int normalizedPage = page == null || page < 0 ? 0 : page;
    int normalizedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    return new PageQuery(normalizedPage, normalizedSize);
  }

  /** 검색 문자열을 정규화합니다. null과 빈 문자열은 전체 조회를 뜻하는 null로 통일합니다. */
  public static String normalizeQuery(String query) {
    if (query == null) {
      return null;
    }
    String trimmed = query.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
