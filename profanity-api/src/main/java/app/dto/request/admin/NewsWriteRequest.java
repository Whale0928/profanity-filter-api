package app.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 소식 작성과 수정 요청입니다.
 *
 * @param title 제목
 * @param category NOTICE, CHANGELOG, MAINTENANCE, ISSUE
 * @param content 마크다운 본문
 * @param status DRAFT 또는 PUBLISHED
 */
public record NewsWriteRequest(
    @NotBlank @Size(max = 160) String title,
    @NotBlank String category,
    @NotBlank String content,
    @NotBlank String status) {}
