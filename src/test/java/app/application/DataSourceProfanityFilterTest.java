package app.application;

import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 금칙어 성능 테스트
 * - 대량(10만개 이상)의 금칙어 키워드 존재시 금칙어 여부 판단에 성능 이슈가 없도록 처리하는 테스트(샘플) 소스
 * - 아호코라식 알고리즘을 활용: https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm
 *
 * @author
 */
class DataSourceProfanityFilterTest {

    private static final int initDummyBadwordCnt = 100_000; //더미용 금칙어 초기화 갯수

    private static final String findBadword = "개새끼"; //테스트용 금칙어
    private static final String findBadword2 = "소새끼"; //테스트용 금칙어2

    //linkedhaset이 contain 성능이 가장 좋음: https://dzone.com/articles/java-collection-performance
    private static LinkedHashSet<String> badwords = new LinkedHashSet<>();
    private static Trie badwordsTrie; //아호코라식용

    @BeforeAll
    static void init() {

        for (int i = 1; i <= initDummyBadwordCnt; i++) {
            String randomBadWord = RandomStringUtils.randomAlphanumeric(30);
            badwords.add(randomBadWord);
        }

        System.out.printf("init 금칙어 갯수(컬렉션용): %d%n", badwords.size());

        //아호코라식용 초기화
        long startInitAho = System.currentTimeMillis();
        badwordsTrie = Trie.builder()
                .addKeywords(badwords)
                .addKeyword(findBadword)
                .addKeyword(findBadword2)
                .build(); //시간이 많이걸리니까 가능하면 초기화 후 재 사용

        long endInitAho = System.currentTimeMillis();
        System.out.println("아호코라식 초기화 소요시간(ms): " + (endInitAho - startInitAho));
    }

    public void initialize() {
        String[] profanities = null;// loadProfanities(); // 데이터베이스나 파일에서 비속어 목록을 로드하는 메소드
        Trie.TrieBuilder builder = Trie.builder().onlyWholeWords().caseInsensitive();
        for (String profanity : profanities) {
            builder.addKeyword(profanity);
        }
        Trie trie = builder.build();
    }

    /**
     * 아호코라식으로도 완전일치 테스트가 가능하지만 java컬렉션을 이용해서도 구현
     */
    @Test
    @Timeout(value = 20, unit = TimeUnit.MILLISECONDS)
    public void 금칙어_완전일치_테스트() {

        badwords.add(findBadword); //테스트용 금칙어를 금칙어 셋에 추가해둠(성능 테스트를 위해 만든 대량의 금칙어에 추가)

        final String notExistBadword = findBadword + System.currentTimeMillis(); //확률적으로 존재할 수 없는 금칙어

        long startExactNano = System.nanoTime();
        long startExactms = System.currentTimeMillis();

        assertTrue(badwords.contains(findBadword));
        assertFalse(badwords.contains(notExistBadword));

        long endExactNano = System.nanoTime();
        long endExactMs = System.currentTimeMillis();

        System.out.println("\n\n완전일치 금칙어 find 소요시간(nano): " + (endExactNano - startExactNano));
        System.out.println("완전일치 금칙어 find 소요시간(ms): " + (endExactMs - startExactms));

    }

    /**
     * 성능을 위해서 포함여부 체크는 아호코라식 알고리즘을 사용
     * - 구현 java 라이브러리: https://github.com/robert-bor/aho-corasick (maven mvnrepository에는 배포를 안하니 참고해서 직접 구현하거나 소스 내려받아서 빌드 후 사용)
     */
    @Test
    @Timeout(value = 20, unit = TimeUnit.MILLISECONDS)
    public void 금칙어_포함여부_아호코라식알고리즘기반_테스트() {

        String targetText_1 = "개새끼들이 뛰어놀고 있어요. 소 는 없어요";
        Collection<Emit> emits_1 = excuteAho(targetText_1);
        assertEquals(1, emits_1.size());

        String targetText_2 = "개새끼들이 뛰어놀고 있어요. 옆에는 소새끼들이 있어요";
        Collection<Emit> emits_2 = excuteAho(targetText_2);
        assertEquals(2, emits_2.size());

        String targetText_3 = "개가 뛰어놀고 있어요. 옆에는 소도 있어요";
        Collection<Emit> emits_3 = excuteAho(targetText_3);
        System.out.println(emits_3);
        assertEquals(0, emits_3.size());
    }

    private Collection<Emit> excuteAho(String targetText) {

        System.out.println("\n===== excuteAho: Start ");
        System.out.println("금칙어가 존재하는지 검사할 텍스트:==>" + targetText);

        long startNano = System.nanoTime();
        long startMs = System.currentTimeMillis();

        Collection<Emit> emits = badwordsTrie.parseText(targetText);
        System.out.println("검출된 금칙어 갯수: " + emits.size());
        for (Emit emit : emits) {
            System.out.printf("  금칙어 '%s'에 매칭됨%n", emit.getKeyword());
        }

        long endNano = System.nanoTime();
        long endMs = System.currentTimeMillis();

        long duNano = endNano - startNano;
        long duMs = endMs - startMs;

        System.out.printf("아호코라식 기반 금칙어 판별 소요시간. '%d(nano)' | '%d(ms)'%n", duNano, duMs);
        System.out.println("===== excuteAho: End ");
        return emits;
    }
}
