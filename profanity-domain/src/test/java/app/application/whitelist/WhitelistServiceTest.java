package app.application.whitelist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.application.whitelist.WhitelistService.WhitelistView;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.InMemoryUserAccountRepository;
import app.domain.InMemoryWhitelistRepository;
import app.domain.user.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WhitelistServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-22T01:00:00Z");

  private final InMemoryWhitelistRepository whitelistRepository = new InMemoryWhitelistRepository();
  private final InMemoryUserAccountRepository userAccountRepository =
      new InMemoryUserAccountRepository();
  private final WhitelistService service =
      new WhitelistService(
          whitelistRepository,
          userAccountRepository,
          new WhitelistReader(whitelistRepository),
          Clock.fixed(NOW, ZoneOffset.UTC));

  private final UUID owner = user("owner@example.test");
  private final UUID stranger = user("stranger@example.test");

  private UUID user(String email) {
    return userAccountRepository.save(UserAccount.create("사용자", email, null, NOW)).getId();
  }

  private static int codeOf(Throwable thrown) {
    return ((BusinessException) thrown).getStatus().code();
  }

  @Nested
  @DisplayName("그룹 만들기는")
  class Create {

    @Test
    @DisplayName("계정당 10개까지 허용하고 11번째는 상한 오류를 낸다")
    void limitsGroupsPerAccount() {
      for (int i = 0; i < 10; i++) {
        service.create(owner, "그룹 " + i, List.of());
      }

      assertThatThrownBy(() -> service.create(owner, "열한 번째", List.of()))
          .satisfies(
              thrown ->
                  assertThat(codeOf(thrown)).isEqualTo(StatusCode.WHITELIST_LIMIT_EXCEEDED.code()));
      assertThat(service.findAll(owner)).hasSize(10);
    }

    @Test
    @DisplayName("다른 계정의 그룹 수는 내 상한에 영향을 주지 않는다")
    void countsPerAccount() {
      for (int i = 0; i < 10; i++) {
        service.create(stranger, "남의 그룹 " + i, List.of());
      }

      WhitelistView created = service.create(owner, "게임 욕설 허용 그룹", List.of("죽여", "처치"));

      assertThat(created.wordCount()).isEqualTo(2);
      assertThat(created.createdAt()).isEqualTo(NOW);
      assertThat(service.findAll(owner))
          .extracting(WhitelistView::id)
          .containsExactly(created.id());
    }

    @Test
    @DisplayName("없는 계정으로는 만들 수 없다")
    void rejectsUnknownAccount() {
      assertThatThrownBy(() -> service.create(UUID.randomUUID(), "그룹", List.of()))
          .satisfies(
              thrown -> assertThat(codeOf(thrown)).isEqualTo(StatusCode.USER_NOT_FOUND.code()));
    }
  }

  @Nested
  @DisplayName("남의 그룹은")
  class Ownership {

    @Test
    @DisplayName("목록에 보이지 않고 수정과 삭제는 없는 그룹과 같은 코드로 거절된다")
    void hidesForeignGroups() {
      WhitelistView mine = service.create(owner, "내 그룹", List.of("죽여"));

      assertThat(service.findAll(stranger)).isEmpty();
      assertThatThrownBy(() -> service.update(stranger, mine.id(), "탈취", List.of()))
          .satisfies(
              thrown ->
                  assertThat(codeOf(thrown)).isEqualTo(StatusCode.WHITELIST_NOT_FOUND.code()));
      assertThatThrownBy(() -> service.delete(stranger, mine.id()))
          .satisfies(
              thrown ->
                  assertThat(codeOf(thrown)).isEqualTo(StatusCode.WHITELIST_NOT_FOUND.code()));
      assertThatThrownBy(() -> service.delete(owner, UUID.randomUUID()))
          .satisfies(
              thrown ->
                  assertThat(codeOf(thrown)).isEqualTo(StatusCode.WHITELIST_NOT_FOUND.code()));
      assertThat(service.findAll(owner))
          .singleElement()
          .extracting(WhitelistView::name)
          .isEqualTo("내 그룹");
    }
  }

  @Test
  @DisplayName("수정은 ID를 유지하고 삭제하면 목록에서 사라진다")
  void updateAndDelete() {
    WhitelistView created = service.create(owner, "게임", List.of("죽여"));

    WhitelistView updated = service.update(owner, created.id(), "게임 채팅", List.of("죽여", "처치"));

    assertThat(updated.id()).isEqualTo(created.id());
    assertThat(updated.name()).isEqualTo("게임 채팅");
    assertThat(updated.words()).containsExactly("죽여", "처치");

    service.delete(owner, created.id());

    assertThat(service.findAll(owner)).isEmpty();
  }
}
