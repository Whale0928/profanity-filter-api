package app.dto.request.admin;

import app.domain.user.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 상태 변경 요청입니다.
 *
 * @param status ACTIVE 또는 DISABLED
 */
public record ChangeUserStatusRequest(@NotNull UserStatus status) {}
