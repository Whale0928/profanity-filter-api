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
}
