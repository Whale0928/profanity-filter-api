package app.application.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.domain.InmemoryProfanityRepository;
import app.domain.profanity.ProfanityWord;
import app.domain.profanity.constant.WordSource;
import app.domain.profanity.constant.isUsedType;
import app.dto.response.FilterResponse;
import app.dto.response.FilterWord;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NormalProfanityFilterTest {

  private static final Logger log = LogManager.getLogger(NormalProfanityFilterTest.class);
  private NormalProfanityFilter normalProfanityFilter;
  private InmemoryProfanityRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InmemoryProfanityRepository();
    normalProfanityFilter = new NormalProfanityFilter(repository);

    repository.save(ProfanityWord.create("욕설"));
    repository.save(ProfanityWord.create("나쁜놈"));
    repository.save(ProfanityWord.create("비속어"));
    normalProfanityFilter.synchronizeProfanityTrie();
  }

  @Test
  @DisplayName("비속어 동기화 작업을 수행할 수 있다.")
  void synchronizeProfanityTrie() {
    repository.deleteAll();

    // when
    normalProfanityFilter.synchronizeProfanityTrie();
    int size1 = normalProfanityFilter.getProfanityTrieList().size();

    repository.save(ProfanityWord.create("비속어"));
    normalProfanityFilter.synchronizeProfanityTrie();
    List<?> list2 = normalProfanityFilter.getProfanityTrieList();

    // then
    assertEquals(0, size1);
    assertEquals(1, list2.size());
  }

  @Test
  @DisplayName("사용 중지된 단어는 Trie에 적재하지 않는다")
  void synchronizeProfanityTrie_excludesUnusedWords() {
    // given
    ProfanityWord disabled = ProfanityWord.create("중지된단어", WordSource.ADMIN, null, Instant.EPOCH);
    disabled.changeUsage(isUsedType.N, null, Instant.EPOCH);
    repository.save(disabled);

    // when
    normalProfanityFilter.synchronizeProfanityTrie();

    // then
    assertFalse(normalProfanityFilter.getProfanityTrieList().contains("중지된단어"));
    assertFalse(normalProfanityFilter.containsProfanity("이건 중지된단어 입니다"));
  }

  @Test
  @DisplayName("적재 단어가 바뀌었는지 동기화 결과로 알려준다")
  void synchronizeProfanityTrie_reportsWhetherWordsChanged() {
    // 같은 사전을 다시 읽으면 변경이 없다고 알린다.
    assertFalse(normalProfanityFilter.synchronizeProfanityTrie());

    repository.save(ProfanityWord.create("추가된단어"));

    assertTrue(normalProfanityFilter.synchronizeProfanityTrie());
    assertFalse(normalProfanityFilter.synchronizeProfanityTrie());
  }

  @Test
  @DisplayName("비속어 등록된 리스트를 조회할 수 있다.")
  void getProfanityTrieList() {
    List<ProfanityWord> allList = repository.findAll();
    // when
    List<?> profanityTrieList = normalProfanityFilter.getProfanityTrieList();
    // then
    assertFalse(profanityTrieList.isEmpty());
    assertEquals(allList.size(), profanityTrieList.size());
  }

  @Test
  @DisplayName("텍스트의 비속어 여부를 확인할 수 있다.")
  void containsProfanity() {
    // given
    String trueText = "이 텍스트에는 욕설이 포함되어 있습니다.";
    String falseText = "이 텍스트에는 포함되어 있습니다.";

    // when
    Boolean trueResult = normalProfanityFilter.containsProfanity(trueText);
    Boolean falseResult = normalProfanityFilter.containsProfanity(falseText);

    // then
    log.info("trueResult : {}", trueResult);
    assertTrue(trueResult);
    log.info("falseResult : {}", falseResult);
    assertFalse(falseResult);
  }

  @Test
  @DisplayName("비속어가 포함된 모든 텍스트를 필터링 할 수 있다.")
  void allMatched() {
    // given
    String text = "'비속어' 이 텍스트에는 욕설이 2번 포함되어 있습니다.";

    // when
    FilterResponse result = normalProfanityFilter.allMatched(text);

    // then
    log.info("result : {}", result);
    assertTrue(result.filterWords().stream().allMatch(filter -> text.contains(filter.word())));
    assertEquals(2, result.filterWords().size());
  }

  @Test
  @DisplayName("비속어가 포함된 첫번째 텍스트를 필터링 할 수 있다.")
  void firstMatched() {
    // given
    String text = "'비속어' 이 텍스트에는 욕설이 2번 포함되어 있습니다.";

    // when
    FilterWord filterWord = normalProfanityFilter.firstMatched(text);

    // then
    log.info("filterWord : {}", filterWord);
    assertNotNull(filterWord);
    assertTrue(text.contains(filterWord.word()));
  }

  @Test
  @DisplayName("null 또는 빈 문자열 입력시 빈 응답을 반환한다")
  void handleEmptyInput() {
    assertEquals(FilterWord.empty(), normalProfanityFilter.firstMatched(null));
    assertEquals(FilterWord.empty(), normalProfanityFilter.firstMatched("   "));

    FilterResponse nullResponse = normalProfanityFilter.allMatched(null);
    FilterResponse emptyResponse = normalProfanityFilter.allMatched("  ");

    assertTrue(nullResponse.filterWords().isEmpty());
    assertTrue(emptyResponse.filterWords().isEmpty());
  }

  @ParameterizedTest
  @DisplayName("특수문자가 포함된 비속어를 필터링할 수 있다")
  @ValueSource(
      strings = {
        "아 진짜 욕!@#설 하고싶네",
        "이런 나쁜!@#놈을 봤나",
        "진짜 빡치네 욕&*&설...",
        "이 비$@속!!어를 어떻게 참냐",
        "욕@#$%설하면서 화를 풀어",
        "나쁜^^^놈 같으니라고",
        "정말 비*&^속!@#어다",
        "이런 욕12#설이란게 말이죠",
        "비!!속##어가 나올 뻔",
        "참 나쁜~~~놈 같구나"
      })
  void handleSpecialCharacters(String text) {
    FilterResponse response = normalProfanityFilter.allMatched(text);
    assertFalse(response.filterWords().isEmpty());
    log.info("Found words: {}", response.filterWords());
  }

  @Nested
  @DisplayName("검출 위치는 원문 기준으로 계산한다")
  class OriginalOffsets {

    @Test
    @DisplayName("앞에 같은 글자로 시작하는 단어가 있어도 검출된 구간만 돌려준다")
    void allMatched_sameLeadingCharBefore_returnsOnlyMatchedRange() {
      // given
      String text = "욕심 많은 욕설";

      // when
      FilterResponse result = normalProfanityFilter.allMatched(text);

      // then
      assertEquals(Set.of(FilterWord.create("욕설", 6, 8)), result.filterWords());
    }

    @Test
    @DisplayName("첫 검출도 앞에 같은 글자로 시작하는 단어를 끌어오지 않는다")
    void firstMatched_sameLeadingCharBefore_returnsOnlyMatchedRange() {
      // when
      FilterWord result = normalProfanityFilter.firstMatched("욕심 많은 욕설");

      // then
      assertEquals(FilterWord.create("욕설", 6, 8), result);
    }

    @Test
    @DisplayName("사이에 끼운 기호는 원문 조각에 그대로 포함한다")
    void allMatched_symbolsInside_keepsOriginalFragment() {
      // when
      FilterResponse result = normalProfanityFilter.allMatched("아 진짜 욕!@#설");

      // then
      assertEquals(Set.of(FilterWord.create("욕!@#설", 5, 10)), result.filterWords());
    }

    @Test
    @DisplayName("같은 단어가 반복되면 각 위치를 따로 돌려준다")
    void allMatched_repeatedWord_returnsEachRange() {
      // when
      FilterResponse result = normalProfanityFilter.allMatched("욕설 욕설");

      // then
      assertEquals(
          Set.of(FilterWord.create("욕설", 0, 2), FilterWord.create("욕설", 3, 5)),
          result.filterWords());
    }

    @ParameterizedTest
    @DisplayName("영문은 대소문자가 달라도 원문 그대로 검출한다")
    @ValueSource(strings = {"fool", "FOOL", "Fool", "fOoL"})
    void matched_englishInAnyCase_returnsOriginalFragment(String word) {
      // given
      repository.save(ProfanityWord.create("fool"));
      normalProfanityFilter.synchronizeProfanityTrie();
      String text = "you " + word;

      // when
      FilterResponse all = normalProfanityFilter.allMatched(text);
      FilterWord first = normalProfanityFilter.firstMatched(text);

      // then
      assertEquals(Set.of(FilterWord.create(word, 4, 8)), all.filterWords());
      assertEquals(FilterWord.create(word, 4, 8), first);
    }
  }
}
