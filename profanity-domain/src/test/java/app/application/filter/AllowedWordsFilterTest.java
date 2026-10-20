package app.application.filter;

import static org.assertj.core.api.Assertions.assertThat;

import app.domain.InmemoryProfanityRepository;
import app.domain.profanity.ProfanityWord;
import app.dto.response.FilterResponse;
import app.dto.response.FilterWord;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AllowedWordsFilterTest {

  private NormalProfanityFilter filter;

  @BeforeEach
  void setUp() {
    InmemoryProfanityRepository repository = new InmemoryProfanityRepository();
    filter = new NormalProfanityFilter(repository);
    repository.save(ProfanityWord.create("죽여"));
    repository.save(ProfanityWord.create("바보"));
    repository.save(ProfanityWord.create("Fool"));
    filter.synchronizeProfanityTrie();
  }

  private Set<String> allowed(String... words) {
    return Set.of(words).stream()
        .map(ProfanityText::comparisonKey)
        .collect(java.util.stream.Collectors.toSet());
  }

  @Nested
  @DisplayName("전체 검출은")
  class AllMatched {

    @Test
    @DisplayName("허용 단어가 없으면 기존 결과와 같다")
    void withoutAllowedWords_matchesLegacyResult() {
      String text = "보스 죽여 이 바보야";

      FilterResponse legacy = filter.allMatched(text);
      FilterResponse withEmpty = filter.allMatched(text, Set.of());

      assertThat(withEmpty.filterWords()).isEqualTo(legacy.filterWords());
      assertThat(legacy.filterWords())
          .extracting(FilterWord::word)
          .containsExactlyInAnyOrder("죽여", "바보");
    }

    @Test
    @DisplayName("허용 단어는 빼고 나머지는 그대로 검출한다")
    void skipsAllowedWordsOnly() {
      FilterResponse result = filter.allMatched("보스 죽여 이 바보야", allowed("죽여"));

      assertThat(result.filterWords()).extracting(FilterWord::word).containsExactly("바보");
    }

    @Test
    @DisplayName("허용 단어 뒤에 오는 검출의 원문 위치가 어긋나지 않는다")
    void keepsPositionsOfFollowingMatches() {
      String text = "죽여 죽여 바보";

      FilterResponse result = filter.allMatched(text, allowed("죽여"));

      assertThat(result.filterWords())
          .singleElement()
          .satisfies(
              word -> {
                assertThat(word.word()).isEqualTo("바보");
                assertThat(text.substring(word.startIndex(), word.endIndex())).isEqualTo("바보");
              });
    }

    @Test
    @DisplayName("원문에 기호가 끼어 있어도 사전 단어 기준으로 허용한다")
    void allowsBySpellingInDictionary() {
      FilterResponse result = filter.allMatched("이런 바-보 같으니", allowed("바-보"));

      assertThat(result.filterWords()).isEmpty();
    }

    @Test
    @DisplayName("영문은 대소문자를 가리지 않고 허용한다")
    void allowsEnglishCaseInsensitively() {
      FilterResponse result = filter.allMatched("you FOOL", allowed("fool"));

      assertThat(result.filterWords()).isEmpty();
    }

    @Test
    @DisplayName("사전에 없는 단어를 허용해도 결과가 바뀌지 않는다")
    void ignoresAllowedWordsNotInDictionary() {
      FilterResponse result = filter.allMatched("이 바보야", allowed("헤드샷"));

      assertThat(result.filterWords()).extracting(FilterWord::word).containsExactly("바보");
    }
  }

  @Nested
  @DisplayName("첫 검출은")
  class FirstMatched {

    @Test
    @DisplayName("허용 단어가 없으면 기존 결과와 같다")
    void withoutAllowedWords_matchesLegacyResult() {
      String text = "보스 죽여 이 바보야";

      assertThat(filter.firstMatched(text, Set.of())).isEqualTo(filter.firstMatched(text));
    }

    @Test
    @DisplayName("첫 검출이 허용 단어이면 그 뒤의 실제 검출을 돌려준다")
    void skipsAllowedFirstMatch() {
      String text = "보스 죽여 이 바보야";

      FilterWord result = filter.firstMatched(text, allowed("죽여"));

      assertThat(result.word()).isEqualTo("바보");
      assertThat(text.substring(result.startIndex(), result.endIndex())).isEqualTo("바보");
    }

    @Test
    @DisplayName("모든 검출이 허용 단어이면 검출 없음으로 돌려준다")
    void returnsEmptyWhenEverythingIsAllowed() {
      FilterWord result = filter.firstMatched("보스 죽여 이 바보야", allowed("죽여", "바보"));

      assertThat(result).isEqualTo(FilterWord.empty());
    }
  }
}
