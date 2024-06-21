package app.application;

import org.ahocorasick.trie.Trie;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.LinkedHashSet;

class NormalProfanityFilterTest {

    private static final LinkedHashSet<String> badwords = new LinkedHashSet<>(); //linkedhaset이 contain 성능이 가장 좋음: https://dzone.com/articles/java-collection-performance
    private Trie ignoreOverlapsTrie;
    private Trie allowOverlapsTrie;

    @BeforeAll
    static void beforeAll() {
        badwords.add("메롱");
        badwords.add("머롱");
        badwords.add("마롤로");
        for (int i = 0; i < 100_000; i++) {
            badwords.add(RandomStringUtils.random(50));
        }
    }

    @BeforeEach
    void setUp() {

        ignoreOverlapsTrie = Trie.builder()
                .ignoreOverlaps()
                .addKeywords(badwords)
                .build();

        long startInitAho = System.currentTimeMillis();

        allowOverlapsTrie = Trie.builder()
                .addKeywords(badwords)
                .build();
        long endInitAho = System.currentTimeMillis();

        System.out.println("아호코라식 초기화 소요시간(ms): " + (endInitAho - startInitAho) / 1_000_000 + "ms");
    }

    @DisplayName("ignoreOverlaps()를 사용하면 중복되는 단어를 무시한다.")
    void test_() throws Exception {
        String text = "메롱머롱마롤로";
        System.out.println("firstMatch : " + ignoreOverlapsTrie.firstMatch(text));
        System.out.println("containsMatch : " + ignoreOverlapsTrie.containsMatch(text));
        System.out.println("tokenize : " + ignoreOverlapsTrie.tokenize(text));
        System.out.println("parseText : " + ignoreOverlapsTrie.parseText(text));
        System.out.println("parseText : " + allowOverlapsTrie.parseText(text));
    }

    @DisplayName("ignoreOverlaps()를 사용하면 중복되는 단어를 무시한다.")
    void test_2() throws Exception {
        String text = "메롱머롱마롤로";

        long start = System.nanoTime();
        System.out.println("firstMatch : " + ignoreOverlapsTrie.firstMatch(text));
        long end = System.nanoTime();

        System.out.println("firstMatch : " + (end - start) / 1_000_000 + "ms");
    }

    @DisplayName("ignoreOverlaps()를 사용하면 중복되는 단어를 무시한다.")
    void test_3() throws Exception {
        String text = "메롱머롱마롤로";
        long start = System.nanoTime();
        System.out.println("parseText : " + allowOverlapsTrie.parseText(text));
        long end = System.nanoTime();
        System.out.println("parseText : " + (end - start) / 1_000_000 + "ms");
    }
}
