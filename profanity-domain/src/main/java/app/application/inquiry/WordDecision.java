package app.application.inquiry;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import java.util.Locale;

/** 관리자가 단어 요청에 내리는 결정입니다. 승인만 사전을 변경하고 거절은 사전을 건드리지 않습니다. */
public enum WordDecision {
  APPROVE,
  REJECT;

  public static WordDecision parse(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "단어 요청 처리 결정은 필수입니다.");
    }
    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "올바르지 않은 단어 요청 처리 결정입니다.");
    }
  }
}
