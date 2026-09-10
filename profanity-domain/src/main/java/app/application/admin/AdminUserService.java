package app.application.admin;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import app.domain.user.UserRole;
import app.domain.user.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 사용자 관리 기능입니다. 역할 변경 API는 제공하지 않습니다. */
@Service
@RequiredArgsConstructor
public class AdminUserService {

  private final UserAccountRepository userAccountRepository;
  private final AdminAuditService adminAuditService;
  private final Clock loginAuthClock;

  @Transactional(readOnly = true)
  public PageResult<AdminUserView> search(String query, UserRole role, PageQuery pageQuery) {
    return userAccountRepository
        .searchForAdmin(PageQuery.normalizeQuery(query), role, pageQuery.page(), pageQuery.size())
        .map(AdminUserView::from);
  }

  /**
   * 사용자 상태를 변경합니다. 자기 계정과 다른 관리자 계정의 비활성화는 금지합니다.
   *
   * @throws BusinessException 대상이 없거나 금지된 상태 변경인 경우
   */
  @Transactional
  public AdminUserView changeStatus(UUID actorId, UUID targetUserId, UserStatus status) {
    UserAccount target =
        userAccountRepository
            .findByIdForUpdate(targetUserId)
            .orElseThrow(() -> new BusinessException(StatusCode.USER_NOT_FOUND));

    Instant now = loginAuthClock.instant();
    if (status == UserStatus.DISABLED) {
      requireDisableAllowed(actorId, target);
      target.disable(now);
    } else {
      target.activate(now);
    }
    UserAccount saved = userAccountRepository.save(target);
    adminAuditService.record(
        actorId,
        AdminAuditAction.USER_STATUS_CHANGED,
        AdminAuditTargetType.USER,
        targetUserId.toString(),
        status.name(),
        now);
    return AdminUserView.from(saved);
  }

  private void requireDisableAllowed(UUID actorId, UserAccount target) {
    if (target.getId().equals(actorId)) {
      throw new BusinessException(StatusCode.USER_STATUS_CHANGE_FORBIDDEN, "자기 계정은 비활성화할 수 없습니다.");
    }
    if (target.isAdmin()) {
      throw new BusinessException(StatusCode.USER_STATUS_CHANGE_FORBIDDEN, "관리자 계정은 비활성화할 수 없습니다.");
    }
  }

  /**
   * 관리자 사용자 항목입니다.
   *
   * @param role CLIENT 또는 ADMIN
   * @param status ACTIVE 또는 DISABLED
   * @param lastLoginAt 마지막 로그인 시각. 로그인 이력이 없으면 null입니다.
   */
  public record AdminUserView(
      UUID id,
      String displayName,
      String primaryEmail,
      String role,
      String status,
      Instant lastLoginAt,
      Instant createdAt) {

    public static AdminUserView from(UserAccount userAccount) {
      return new AdminUserView(
          userAccount.getId(),
          userAccount.getDisplayName(),
          userAccount.getPrimaryEmail(),
          userAccount.getRole().name(),
          userAccount.getStatus().name(),
          userAccount.getLastLoginAt(),
          userAccount.getCreatedAt());
    }
  }
}
