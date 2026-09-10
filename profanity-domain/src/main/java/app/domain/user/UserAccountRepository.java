package app.domain.user;

import app.domain.support.PageResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository {
  Optional<UserAccount> findById(UUID id);

  Optional<UserAccount> findByIdForUpdate(UUID id);

  Optional<UserAccount> findByPrimaryEmailForUpdate(String primaryEmail);

  UserAccount save(UserAccount userAccount);

  List<UserAccount> findAllByIdIn(Collection<UUID> ids);

  /**
   * 관리자 사용자 목록을 조회합니다.
   *
   * @param query 표시 이름과 대표 이메일에 대한 부분 일치 검색어. null이면 전체입니다.
   * @param role 역할 필터. null이면 전체입니다.
   */
  PageResult<UserAccount> searchForAdmin(String query, UserRole role, int page, int size);
}
