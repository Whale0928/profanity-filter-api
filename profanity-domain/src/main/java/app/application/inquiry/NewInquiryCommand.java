package app.application.inquiry;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.inquiry.InquiryType;
import app.domain.manage.WordRequestType;
import java.util.Locale;
import java.util.Set;

/**
 * 로그인 사용자가 등록하는 새 문의입니다.
 *
 * <p>단어 요청이면 word, requestType, severity가 모두 필요합니다. 일반 문의라면 세 값은 무시합니다.
 *
 * @param type 문의 유형
 * @param title 제목
 * @param content 문의 내용
 * @param word 요청 단어
 * @param requestType ADD, REMOVE, MODIFY
 * @param severity LOW, MEDIUM, HIGH
 */
public record NewInquiryCommand(
    InquiryType type,
    String title,
    String content,
    String word,
    WordRequestType requestType,
    String severity) {

  private static final Set<String> SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH");

  public static NewInquiryCommand of(
      String type, String title, String content, String word, String requestType, String severity) {
    InquiryType inquiryType = InquiryType.parse(type);
    if (inquiryType != InquiryType.WORD_REQUEST) {
      return new NewInquiryCommand(inquiryType, title, content, null, null, null);
    }
    return new NewInquiryCommand(
        inquiryType,
        title,
        content,
        requireWord(word),
        requireRequestType(requestType),
        requireSeverity(severity));
  }

  public boolean isWordRequest() {
    return type == InquiryType.WORD_REQUEST;
  }

  private static String requireWord(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "단어 요청은 대상 단어가 필요합니다.");
    }
    return value.trim();
  }

  private static WordRequestType requireRequestType(String value) {
    WordRequestType requestType = WordRequestType.fromStored(value);
    if (requestType == null) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "올바르지 않은 단어 요청 타입입니다.");
    }
    return requestType;
  }

  private static String requireSeverity(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "단어 요청은 심각도가 필요합니다.");
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    if (!SEVERITIES.contains(normalized)) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "올바르지 않은 심각도입니다.");
    }
    return normalized;
  }
}
