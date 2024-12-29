package app.application.filter;

import app.application.event.FakeApplicationEventPublisher;
import app.core.data.response.ApiResponse;
import app.core.data.response.Detected;
import app.domain.InmemoryProfanityRepository;
import app.domain.profanity.ProfanityWord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultProfanityHandlerTest {

    private static final Logger log = LogManager.getLogger(DefaultProfanityHandlerTest.class);
    private ProfanityHandler ProfanityHandler;

    @BeforeEach
    void setUp() {
        InmemoryProfanityRepository repository = new InmemoryProfanityRepository();
        QuickProfanityFilter quickProfanityFilter = new QuickProfanityFilter();
        NormalProfanityFilter normalProfanityFilter = new NormalProfanityFilter(repository);
        FakeApplicationEventPublisher eventPublisher = new FakeApplicationEventPublisher();

        repository.save(ProfanityWord.create("욕설"));
        repository.save(ProfanityWord.create("나쁜놈"));
        repository.save(ProfanityWord.create("비속어"));
        normalProfanityFilter.synchronizeProfanityTrie();

        ProfanityHandler = new DefaultProfanityHandler(quickProfanityFilter, normalProfanityFilter, eventPublisher);
    }


    @Test
    @DisplayName("quick 타입으로 비속어 필터링할 수 있다.")
    void test_1() {
        // given
        String word = "'씨뻘'욕설이 들어간 문장입니다.";

        // when
        ApiResponse response = ProfanityHandler.quickFilter(word);

        // then
        Set<Detected> detected = response.detected();
        assertEquals(1, detected.size());
        assertTrue(detected.stream().anyMatch(d -> d.filteredWord().equals("씨뻘") && d.length() == 2));
    }

    @Test
    @DisplayName("normal 타입으로 비속어 필터링할 수 있다.")
    void test_2() {
        // given
        String word = "욕설이 들어간 문장입니다.";

        // when
        ApiResponse response = ProfanityHandler.normalFilter(word);

        // then
        log.info("응답 : {}", response);
        Set<Detected> detected = response.detected();
        assertEquals(1, detected.size());
        assertTrue(detected.stream().anyMatch(d -> d.filteredWord().equals("욕설") && d.length() == 2));
    }

    @Test
    @DisplayName("filter 타입으로 비속어 필터링할 수 있다. 마스킹 처리가 된다. ")
    void test_3() {
        // given
        String word = "욕설이 들어간 문장입니다.";

        // when
        ApiResponse response = ProfanityHandler.sanitizeProfanity(word);

        // then
        log.info("응답 : {}", response);
        String filtered = response.filtered();
        Detected detected = response.detected().stream().findFirst().orElseThrow();
        assertTrue(detected.filteredWord().equals("욕설") && detected.length() == 2);
        assertEquals("**이 들어간 문장입니다.", filtered);
    }
}
