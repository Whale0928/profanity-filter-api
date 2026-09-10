package app.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserAccountTest {

  private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");

  @Test
  @DisplayName("사용자 생성 시 대표 이메일을 trim하고 소문자로 정규화한다")
  void create_whenPrimaryEmailHasMixedCaseAndSpaces_normalizesEmail() {
    UserAccount user = UserAccount.create("Tester", " Tester@Example.COM ", null, NOW);

    assertThat(user.getPrimaryEmail()).isEqualTo("tester@example.com");
  }

  @Test
  @DisplayName("사용자 생성 시 대표 이메일이 비어 있으면 거부한다")
  void create_whenPrimaryEmailIsBlank_throwsException() {
    assertThatThrownBy(() -> UserAccount.create("Tester", "  ", null, NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("primaryEmail must not be blank");
  }

  @Test
  @DisplayName("프로필 동기화 시 대표 이메일이 null이면 거부한다")
  void synchronizeProfile_whenPrimaryEmailIsNull_throwsException() {
    UserAccount user = UserAccount.create("Tester", "tester@example.com", null, NOW);

    assertThatThrownBy(() -> user.synchronizeProfile("Changed", null, null, NOW.plusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("primaryEmail must not be blank");
    assertThat(user.getDisplayName()).isEqualTo("Tester");
    assertThat(user.getPrimaryEmail()).isEqualTo("tester@example.com");
  }

  @Test
  @DisplayName("새로 만든 사용자는 CLIENT 역할이며 관리자가 아니다")
  void create_assignsClientRoleByDefault() {
    UserAccount user = UserAccount.create("Tester", "tester@example.com", null, NOW);

    assertThat(user.getRole()).isEqualTo(UserRole.CLIENT);
    assertThat(user.isAdmin()).isFalse();
    assertThat(user.getLastLoginAt()).isNull();
  }

  @Test
  @DisplayName("로그인 시각을 기록하면 수정 시각도 함께 갱신한다")
  void recordLogin_updatesLastLoginAndUpdatedAt() {
    UserAccount user = UserAccount.create("Tester", "tester@example.com", null, NOW);
    Instant loginAt = NOW.plusSeconds(3600);

    user.recordLogin(loginAt);

    assertThat(user.getLastLoginAt()).isEqualTo(loginAt);
    assertThat(user.getUpdatedAt()).isEqualTo(loginAt);
  }

  @Test
  @DisplayName("비활성화한 계정을 다시 활성화할 수 있다")
  void activate_restoresActiveStatus() {
    UserAccount user = UserAccount.create("Tester", "tester@example.com", null, NOW);
    user.disable(NOW.plusSeconds(1));

    user.activate(NOW.plusSeconds(2));

    assertThat(user.isActive()).isTrue();
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(user.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
  }
}
