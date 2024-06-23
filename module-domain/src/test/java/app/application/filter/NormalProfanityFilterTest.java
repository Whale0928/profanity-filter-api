package app.application.filter;

import app.domain.InmemoryProfanityRepository;
import app.domain.ProfanityWord;
import app.dto.response.FilterResponse;
import app.dto.response.FilterWord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        //then
        assertEquals(0, size1);
        assertEquals(1, list2.size());
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
        assertTrue(
                result.filterWords()
                        .stream()
                        .allMatch(filter -> text.contains(filter.word()))
        );
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

}
