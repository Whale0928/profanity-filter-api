package app.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilterWordTest {
    @Test
    @DisplayName("문자열을 필터링할 수 있다.")
    void create() {
        // given
        String word = "안녕하세요";
        Integer start = 0;
        Integer end = 2;

        // when
        FilterWord filterWord = FilterWord.create(word, start, end);
        System.out.println(filterWord);
        // then
        assertEquals(word, filterWord.word());
        assertEquals(start, filterWord.startIndex());
        assertEquals(end, filterWord.endIndex());
    }

}
