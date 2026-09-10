package app.dto.response;

import java.util.UUID;

/**
 * 로그인 사용자 정보입니다.
 *
 * @param role CLIENT 또는 ADMIN
 * @param admin 관리자 메뉴 노출 여부. role이 ADMIN이면 true입니다.
 */
public record LoginUserResponse(
    UUID id, String displayName, String email, String avatarUrl, String role, boolean admin) {}
