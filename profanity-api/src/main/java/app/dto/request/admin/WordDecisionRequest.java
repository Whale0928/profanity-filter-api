package app.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 단어 요청 승인과 거절 요청입니다.
 *
 * @param decision APPROVE 또는 REJECT
 * @param reason 감사 기록에 남길 판단 사유
 */
public record WordDecisionRequest(@NotBlank String decision, @Size(max = 500) String reason) {}
