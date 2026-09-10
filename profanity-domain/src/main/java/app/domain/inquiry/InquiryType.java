package app.domain.inquiry;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import java.util.Locale;

/** 문의 유형입니다. WORD_REQUEST는 사전 단어 요청이고 GENERAL은 그 밖의 서비스 문의입니다. */
public enum InquiryType {
  WORD_REQUEST,
  GENERAL;

  /** 목록 필터에 쓰는 파싱입니다. null과 빈 문자열은 전체 조회를 뜻하는 null로 통일합니다. */
  public static InquiryType parseFilter(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return parse(value);
  }

  /** 쓰기 요청에 쓰는 파싱입니다. 값이 없거나 정의되지 않았으면 예외가 발생합니다. */
  public static InquiryType parse(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "문의 유형은 필수입니다.");
    }
    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "올바르지 않은 문의 유형입니다.");
    }
  }
}
