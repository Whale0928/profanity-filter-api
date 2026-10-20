package app.domain.whitelist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WhitelistTest {

  private static final Instant NOW = Instant.parse("2026-09-22T01:00:00Z");
  private static final UUID OWNER = UUID.randomUUID();

  private static int codeOf(Throwable thrown) {
    return ((BusinessException) thrown).getStatus().code();
  }

  @Nested
  @DisplayName("허용 단어는")
  class Words {

    @Test
    @DisplayName("앞뒤 공백과 빈 값을 지우고 입력한 순서를 유지한다")
    void trimsAndKeepsOrder() {
      List<String> input = new ArrayList<>(List.of("  죽여 ", "", "   ", "처치"));
      input.add(null);

      Whitelist whitelist = Whitelist.create(OWNER, "게임", input, NOW);

      assertThat(whitelist.getWords()).containsExactly("죽여", "처치");
    }

    @Test
    @DisplayName("비교 키가 같은 단어는 처음 것만 남긴다")
    void removesDuplicatesByComparisonKey() {
      Whitelist whitelist =
          Whitelist.create(OWNER, "게임", List.of("Fool", "fool", "F-O-O-L", "바보", "바-보"), NOW);

      assertThat(whitelist.getWords()).containsExactly("Fool", "바보");
      assertThat(whitelist.comparisonKeys()).containsExactlyInAnyOrder("fool", "바보");
    }

    @Test
    @DisplayName("200개까지 담고 넘으면 상한 오류를 낸다")
    void rejectsMoreThanLimit() {
      List<String> atLimit = IntStream.range(0, 200).mapToObj(WhitelistTest::word).toList();
      List<String> overLimit = IntStream.range(0, 201).mapToObj(WhitelistTest::word).toList();

      assertThat(Whitelist.create(OWNER, "게임", atLimit, NOW).getWords()).hasSize(200);
      assertThatThrownBy(() -> Whitelist.create(OWNER, "게임", overLimit, NOW))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              thrown ->
                  assertThat(codeOf(thrown)).isEqualTo(StatusCode.WHITELIST_LIMIT_EXCEEDED.code()));
    }

    @Test
    @DisplayName("중복을 뺀 뒤의 개수로 상한을 센다")
    void countsAfterRemovingDuplicates() {
      List<String> withDuplicates = new ArrayList<>();
      IntStream.range(0, 200).mapToObj(WhitelistTest::word).forEach(withDuplicates::add);
      IntStream.range(0, 50).mapToObj(WhitelistTest::word).forEach(withDuplicates::add);

      assertThat(Whitelist.create(OWNER, "게임", withDuplicates, NOW).getWords()).hasSize(200);
    }

    @Test
    @DisplayName("필터가 절대 검출하지 않는 숫자나 기호만의 단어는 거절한다")
    void rejectsWordsTheFilterNeverMatches() {
      for (String useless : List.of("1234", "!!!", "😀", "12-34")) {
        assertThatThrownBy(() -> Whitelist.create(OWNER, "게임", List.of(useless), NOW))
            .as(useless)
            .isInstanceOf(BusinessException.class)
            .satisfies(
                thrown -> assertThat(codeOf(thrown)).isEqualTo(StatusCode.BAD_REQUEST.code()));
      }
    }

    @Test
    @DisplayName("80자를 넘거나 줄바꿈이 든 단어는 거절한다")
    void rejectsTooLongOrMultilineWords() {
      assertThatThrownBy(() -> Whitelist.create(OWNER, "게임", List.of("가".repeat(81)), NOW))
          .isInstanceOf(BusinessException.class);
      assertThatThrownBy(() -> Whitelist.create(OWNER, "게임", List.of("죽\n여"), NOW))
          .isInstanceOf(BusinessException.class);
      assertThat(Whitelist.create(OWNER, "게임", List.of("가".repeat(80)), NOW).getWords()).hasSize(1);
    }

    @Test
    @DisplayName("단어가 없어도 그룹을 만들 수 있다")
    void allowsEmptyGroup() {
      assertThat(Whitelist.create(OWNER, "빈 그룹", null, NOW).getWords()).isEmpty();
      assertThat(Whitelist.create(OWNER, "빈 그룹", List.of(), NOW).comparisonKeys()).isEmpty();
    }
  }

  @Nested
  @DisplayName("그룹 이름은")
  class Name {

    @Test
    @DisplayName("비어 있거나 60자를 넘으면 거절한다")
    void rejectsBlankOrTooLong() {
      for (String name : new String[] {null, "", "   ", "가".repeat(61)}) {
        assertThatThrownBy(() -> Whitelist.create(OWNER, name, List.of(), NOW))
            .isInstanceOf(BusinessException.class)
            .satisfies(
                thrown -> assertThat(codeOf(thrown)).isEqualTo(StatusCode.BAD_REQUEST.code()));
      }
      assertThat(Whitelist.create(OWNER, " " + "가".repeat(60) + " ", List.of(), NOW).getName())
          .hasSize(60);
    }
  }

  @Test
  @DisplayName("수정해도 그룹 ID와 만든 시각은 그대로다")
  void update_keepsIdentity() {
    Whitelist whitelist = Whitelist.create(OWNER, "게임", List.of("죽여"), NOW);
    UUID id = whitelist.getId();

    whitelist.update("친구창", List.of("바보"), NOW.plusSeconds(60));

    assertThat(whitelist.getId()).isEqualTo(id);
    assertThat(whitelist.getName()).isEqualTo("친구창");
    assertThat(whitelist.getWords()).containsExactly("바보");
    assertThat(whitelist.getCreatedAt()).isEqualTo(NOW);
    assertThat(whitelist.getUpdatedAt()).isEqualTo(NOW.plusSeconds(60));
  }

  /** 서로 비교 키가 다른 한글 단어를 만든다. 숫자는 비교 키에서 지워지므로 쓰지 않는다. */
  private static String word(int index) {
    return "단어" + (char) ('가' + index);
  }
}
