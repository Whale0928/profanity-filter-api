package app.application.whitelist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.InMemoryApiKeyRepository;
import app.domain.InMemoryWhitelistRepository;
import app.domain.apikey.ApiKey;
import app.domain.whitelist.Whitelist;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WhitelistResolverTest {

  private static final Instant NOW = Instant.parse("2026-09-22T01:00:00Z");
  private static final UUID OWNER = UUID.randomUUID();
  private static final UUID STRANGER = UUID.randomUUID();
  private static final String OWNER_KEY = "hash-owner";

  private final InMemoryApiKeyRepository apiKeyRepository = new InMemoryApiKeyRepository();
  private final InMemoryWhitelistRepository whitelistRepository = new InMemoryWhitelistRepository();
  private final WhitelistResolver resolver =
      new WhitelistResolver(apiKeyRepository, new WhitelistReader(whitelistRepository));

  private Whitelist game;
  private Whitelist friends;
  private Whitelist strangers;

  @BeforeEach
  void setUp() {
    apiKeyRepository.save(
        ApiKey.issue(
            OWNER,
            "서비스",
            "owner@example.test",
            OWNER_KEY,
            "hint",
            "test",
            null,
            LocalDateTime.of(2026, 9, 1, 9, 0)));
    game = whitelistRepository.save(Whitelist.create(OWNER, "게임", List.of("죽여", "처치"), NOW));
    friends = whitelistRepository.save(Whitelist.create(OWNER, "친구창", List.of("바보", "죽여"), NOW));
    strangers = whitelistRepository.save(Whitelist.create(STRANGER, "남의 그룹", List.of("멍청이"), NOW));
  }

  private static int codeOf(Throwable thrown) {
    return ((BusinessException) thrown).getStatus().code();
  }

  @Test
  @DisplayName("그룹을 지정하지 않으면 API Key를 조회하지 않고 빈 집합을 돌려준다")
  void withoutIds_returnsEmpty() {
    assertThat(resolver.resolve("unknown-hash", null)).isEmpty();
    assertThat(resolver.resolve("unknown-hash", List.of())).isEmpty();
  }

  @Test
  @DisplayName("여러 그룹을 지정하면 허용 단어를 합친다")
  void unionsWords() {
    assertThat(resolver.resolve(OWNER_KEY, List.of(game.getId(), friends.getId())))
        .containsExactlyInAnyOrder("죽여", "처치", "바보");
  }

  @Test
  @DisplayName("같은 ID를 반복해도 하나로 센다")
  void countsDistinctIds() {
    List<UUID> repeated = new ArrayList<>();
    for (int i = 0; i < 8; i++) repeated.add(game.getId());

    assertThat(resolver.resolve(OWNER_KEY, repeated)).containsExactlyInAnyOrder("죽여", "처치");
  }

  @Test
  @DisplayName("서로 다른 그룹이 5개를 넘으면 상한 오류를 낸다")
  void rejectsMoreThanFiveGroups() {
    List<UUID> ids = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      ids.add(whitelistRepository.save(Whitelist.create(OWNER, "그룹" + i, List.of(), NOW)).getId());
    }

    assertThatThrownBy(() -> resolver.resolve(OWNER_KEY, ids))
        .satisfies(
            thrown ->
                assertThat(codeOf(thrown)).isEqualTo(StatusCode.WHITELIST_LIMIT_EXCEEDED.code()));
    assertThat(resolver.resolve(OWNER_KEY, ids.subList(0, 5))).isEmpty();
  }

  @Test
  @DisplayName("없는 그룹과 남의 그룹을 같은 코드로 거절한다")
  void rejectsMissingAndForeignGroupsAlike() {
    assertThatThrownBy(() -> resolver.resolve(OWNER_KEY, List.of(UUID.randomUUID())))
        .satisfies(
            thrown -> assertThat(codeOf(thrown)).isEqualTo(StatusCode.WHITELIST_NOT_FOUND.code()));
    assertThatThrownBy(() -> resolver.resolve(OWNER_KEY, List.of(strangers.getId())))
        .satisfies(
            thrown -> assertThat(codeOf(thrown)).isEqualTo(StatusCode.WHITELIST_NOT_FOUND.code()));
  }

  @Test
  @DisplayName("내 그룹에 남의 그룹이 하나라도 섞이면 전체를 거절한다")
  void rejectsWholeRequestWhenOneIdIsForeign() {
    assertThatThrownBy(() -> resolver.resolve(OWNER_KEY, List.of(game.getId(), strangers.getId())))
        .satisfies(
            thrown -> assertThat(codeOf(thrown)).isEqualTo(StatusCode.WHITELIST_NOT_FOUND.code()));
  }

  @Test
  @DisplayName("소유 계정이 없는 API Key는 그룹을 쓸 수 없다")
  void rejectsKeyWithoutOwner() {
    ApiKey unowned =
        ApiKey.issue(
            OWNER,
            "레거시",
            "legacy@example.test",
            "hash-legacy",
            "hint",
            "test",
            null,
            LocalDateTime.of(2025, 1, 1, 9, 0));
    apiKeyRepository.save(unowned);
    clearOwner(unowned);

    assertThatThrownBy(() -> resolver.resolve("hash-legacy", List.of(game.getId())))
        .satisfies(
            thrown ->
                assertThat(codeOf(thrown)).isEqualTo(StatusCode.WHITELIST_OWNER_REQUIRED.code()));
    assertThatThrownBy(() -> resolver.resolve("hash-unknown", List.of(game.getId())))
        .satisfies(
            thrown ->
                assertThat(codeOf(thrown)).isEqualTo(StatusCode.WHITELIST_OWNER_REQUIRED.code()));
  }

  /** 소유 계정이 연결되기 전의 과거 키를 흉내 낸다. 발급 경로로는 만들 수 없는 상태다. */
  private static void clearOwner(ApiKey apiKey) {
    try {
      var field = ApiKey.class.getDeclaredField("userId");
      field.setAccessible(true);
      field.set(apiKey, null);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
