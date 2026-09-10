package app.dto.request.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * 문의 답변 등록 요청입니다.
 *
 * @param content 답변 내용
 * @param resolve true이면 답변과 함께 문의를 완료로 옮깁니다. 사전은 변경하지 않습니다.
 */
public record InquiryReplyRequest(@NotBlank String content, boolean resolve) {}
