package app.domain.news;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import java.util.Locale;

/** 소식 분류입니다. 공지, 변경 내역, 점검 안내, 알려진 이슈 네 가지를 사용합니다. */
public enum NewsCategory {
  NOTICE,
  CHANGELOG,
  MAINTENANCE,
  ISSUE;

  /** 목록 필터에 쓰는 파싱입니다. null과 빈 문자열은 전체 조회를 뜻하는 null로 통일합니다. */
  public static NewsCategory parseFilter(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return parse(value);
  }

  /** 쓰기 요청에 쓰는 파싱입니다. 값이 없거나 정의되지 않았으면 예외가 발생합니다. */
  public static NewsCategory parse(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "소식 분류는 필수입니다.");
    }
    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "올바르지 않은 소식 분류입니다.");
    }
  }
}
