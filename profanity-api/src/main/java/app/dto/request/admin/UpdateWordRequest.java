package app.dto.request.admin;

import app.domain.profanity.constant.isUsedType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 사전 단어 수정 요청입니다.
 *
 * @param isUsed Y 또는 N. N으로 바꾸면 재동기화 이후 필터 매칭에서 제외됩니다.
 */
public record UpdateWordRequest(
    @NotBlank @Size(max = 255) String word, @NotNull isUsedType isUsed) {}
