package app.domain.inquiry;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import java.util.Locale;

/** 문의 처리 상태입니다. 접수, 처리 중, 완료 세 단계를 사용합니다. */
public enum InquiryStatus {
  RECEIVED,
  IN_PROGRESS,
  RESOLVED;

  /** 목록 필터에 쓰는 파싱입니다. null과 빈 문자열은 전체 조회를 뜻하는 null로 통일합니다. */
  public static InquiryStatus parseFilter(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return parse(value);
  }

  /** 쓰기 요청에 쓰는 파싱입니다. 값이 없거나 정의되지 않았으면 예외가 발생합니다. */
  public static InquiryStatus parse(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "문의 상태는 필수입니다.");
    }
    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "올바르지 않은 문의 상태입니다.");
    }
  }
}
