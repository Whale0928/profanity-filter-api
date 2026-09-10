package app.dto.request.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * 문의 상태 변경 요청입니다.
 *
 * @param status RECEIVED, IN_PROGRESS, RESOLVED
 */
public record ChangeInquiryStatusRequest(@NotBlank String status) {}
