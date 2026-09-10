package app.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.application.admin.AdminUserService.AdminUserView;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.InMemoryAdminAuditLogRepository;
import app.domain.InMemoryUserAccountRepository;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import app.domain.user.UserAccount;
import app.domain.user.UserRole;
import app.domain.user.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminUserServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-10T03:00:00Z");

  private final InMemoryUserAccountRepository userAccountRepository =
      new InMemoryUserAccountRepository();
  private final InMemoryAdminAuditLogRepository auditLogRepository =
      new InMemoryAdminAuditLogRepository();
  private final AdminUserService adminUserService =
      new AdminUserService(
          userAccountRepository,
          new AdminAuditService(auditLogRepository),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Nested
  @DisplayName("사용자 목록 조회는")
  class Search {

    @Test
    @DisplayName("표시 이름과 이메일 부분 일치로 검색한다")
    void search_withQuery_matchesNameAndEmail() {
      save("김비속", "profanity@example.test");
      save("이관리", "manager@example.test");

      PageResult<AdminUserView> byName = adminUserService.search("김비속", null, PageQuery.of(0, 20));
      PageResult<AdminUserView> byEmail =
          adminUserService.search("manager", null, PageQuery.of(0, 20));

      assertThat(byName.items()).extracting(AdminUserView::displayName).containsExactly("김비속");
      assertThat(byEmail.items())
          .extracting(AdminUserView::primaryEmail)
          .containsExactly("manager@example.test");
    }

    @Test
    @DisplayName("빈 검색어는 전체 조회로 처리한다")
    void search_withBlankQuery_returnsAll() {
      save("김비속", "profanity@example.test");
      save("이관리", "manager@example.test");

      assertThat(adminUserService.search("   ", null, PageQuery.of(0, 20)).items()).hasSize(2);
    }

    @Test
    @DisplayName("역할 필터로 관리자만 조회한다")
    void search_withRole_filtersByRole() {
      save("김비속", "profanity@example.test");
      InMemoryUserAccountRepository.asAdmin(save("이관리", "manager@example.test"));

      PageResult<AdminUserView> admins =
          adminUserService.search(null, UserRole.ADMIN, PageQuery.of(0, 20));

      assertThat(admins.items()).extracting(AdminUserView::role).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("다음 페이지가 있으면 hasNext를 참으로 돌려준다")
    void search_whenMoreRemains_reportsHasNext() {
      save("사용자일", "one@example.test");
      save("사용자이", "two@example.test");
      save("사용자삼", "three@example.test");

      PageResult<AdminUserView> first = adminUserService.search(null, null, PageQuery.of(0, 2));
      PageResult<AdminUserView> second = adminUserService.search(null, null, PageQuery.of(1, 2));

      assertThat(first.items()).hasSize(2);
      assertThat(first.hasNext()).isTrue();
      assertThat(second.items()).hasSize(1);
      assertThat(second.hasNext()).isFalse();
      assertThat(second.page()).isEqualTo(1);
    }

    @Test
    @DisplayName("결과가 없으면 빈 목록을 돌려준다")
    void search_whenNoMatch_returnsEmptyItems() {
      save("김비속", "profanity@example.test");

      assertThat(adminUserService.search("없는이름", null, PageQuery.of(0, 20)).items()).isEmpty();
    }
  }

  @Nested
  @DisplayName("사용자 상태 변경은")
  class ChangeStatus {

    @Test
    @DisplayName("일반 사용자를 비활성화하고 감사 기록을 남긴다")
    void changeStatus_disableClient_recordsAudit() {
      UserAccount actor =
          InMemoryUserAccountRepository.asAdmin(save("이관리", "manager@example.test"));
      UserAccount target = save("김비속", "profanity@example.test");

      AdminUserView changed =
          adminUserService.changeStatus(actor.getId(), target.getId(), UserStatus.DISABLED);

      assertThat(changed.status()).isEqualTo("DISABLED");
      assertThat(target.isActive()).isFalse();
      assertThat(auditLogRepository.findAll()).hasSize(1);
      assertThat(auditLogRepository.findAll().get(0).getAction())
          .isEqualTo(AdminAuditAction.USER_STATUS_CHANGED);
      assertThat(auditLogRepository.findAll().get(0).getTargetId())
          .isEqualTo(target.getId().toString());
      assertThat(auditLogRepository.findAll().get(0).getActorUserId()).isEqualTo(actor.getId());
      assertThat(auditLogRepository.findAll().get(0).getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("비활성 사용자를 다시 활성화한다")
    void changeStatus_activate_restoresAccess() {
      UserAccount actor =
          InMemoryUserAccountRepository.asAdmin(save("이관리", "manager@example.test"));
      UserAccount target = save("김비속", "profanity@example.test");
      target.disable(NOW);

      AdminUserView changed =
          adminUserService.changeStatus(actor.getId(), target.getId(), UserStatus.ACTIVE);

      assertThat(changed.status()).isEqualTo("ACTIVE");
      assertThat(target.isActive()).isTrue();
    }

    @Test
    @DisplayName("자기 계정 비활성화를 거부한다")
    void changeStatus_selfDisable_isRejected() {
      UserAccount actor =
          InMemoryUserAccountRepository.asAdmin(save("이관리", "manager@example.test"));

      assertThatThrownBy(
              () ->
                  adminUserService.changeStatus(actor.getId(), actor.getId(), UserStatus.DISABLED))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.USER_STATUS_CHANGE_FORBIDDEN.code());
      assertThat(actor.isActive()).isTrue();
      assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("다른 관리자 계정 비활성화를 거부한다")
    void changeStatus_disableAnotherAdmin_isRejected() {
      UserAccount actor =
          InMemoryUserAccountRepository.asAdmin(save("이관리", "manager@example.test"));
      UserAccount other = InMemoryUserAccountRepository.asAdmin(save("박관리", "other@example.test"));

      assertThatThrownBy(
              () ->
                  adminUserService.changeStatus(actor.getId(), other.getId(), UserStatus.DISABLED))
          .isInstanceOf(BusinessException.class);
      assertThat(other.isActive()).isTrue();
      assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("없는 사용자는 USER_NOT_FOUND로 처리한다")
    void changeStatus_missingUser_throwsUserNotFound() {
      UserAccount actor =
          InMemoryUserAccountRepository.asAdmin(save("이관리", "manager@example.test"));

      assertThatThrownBy(
              () ->
                  adminUserService.changeStatus(
                      actor.getId(), UUID.randomUUID(), UserStatus.DISABLED))
          .isInstanceOf(BusinessException.class)
          .extracting(exception -> ((BusinessException) exception).getStatus().code())
          .isEqualTo(StatusCode.USER_NOT_FOUND.code());
    }
  }

  private UserAccount save(String displayName, String email) {
    return userAccountRepository.save(UserAccount.create(displayName, email, null, NOW));
  }
}
