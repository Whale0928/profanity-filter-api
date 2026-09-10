package app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 로그인 사용자가 등록하는 새 문의입니다.
 *
 * <p>단어 요청이면 word, requestType, severity가 함께 필요합니다. 일반 문의라면 세 값은 비워 둡니다.
 *
 * @param type WORD_REQUEST 또는 GENERAL
 * @param title 제목
 * @param content 문의 내용. 단어 요청에서는 요청 사유로도 사용합니다.
 * @param word 요청 단어
 * @param requestType ADD, REMOVE, MODIFY
 * @param severity LOW, MEDIUM, HIGH
 */
public record CreateInquiryRequest(
    @NotBlank String type,
    @NotBlank @Size(max = 160) String title,
    @NotBlank String content,
    @Size(max = 255) String word,
    String requestType,
    String severity) {}
